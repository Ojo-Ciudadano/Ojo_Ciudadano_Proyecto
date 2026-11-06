/// <reference types="node" />
import https from 'node:https';
import fs from 'node:fs';
import path from 'node:path';
import axios, { type AxiosInstance, type AxiosResponse } from 'axios';

// Ignorar certificados TLS autofirmados (uct-ingress-tls en Kubernetes UCT)
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

interface JwtPayload {
  sub?: string;
  email?: string;
  roles?: string[];
}

interface CheckResult {
  step: string;
  description: string;
  method: string;
  path: string;
  url: string;
  expectedStatus: number;
  actualStatus: number;
  passed: boolean;
  details?: Record<string, unknown>;
}

interface E2EReport {
  issue: string;
  runTag: string;
  executedAt: string;
  environment: {
    apiGatewayBaseUrl: string;
    tlsRejectUnauthorizedDisabled: boolean;
  };
  actors: {
    ciudadano: {
      id: string;
      email: string;
      roles: string[];
    };
    administrador: {
      id: string;
      email: string;
      roles: string[];
    };
  };
  persistedData: {
    incidenteId: string;
    titulo: string;
    categoriaId: string;
    categoriaNombre: string;
    comuna: string;
    estadoInicial: string;
    estadoConfirmado: string;
    observacionCambioEstado: string;
    historialRegistroId: string;
    historialFechaCambio: string;
    totalRegistrosHistorial: number;
  };
  checks: CheckResult[];
  summary: {
    totalChecks: number;
    passedChecks: number;
    failedChecks: number;
    status: 'PASSED' | 'FAILED';
    errorMessage?: string;
  };
}

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function normalizeApiBaseUrl(rawUrl: string): string {
  const trimmed = rawUrl.trim().replace(/\/+$/, '');
  if (trimmed.endsWith('/api/v1')) {
    return trimmed;
  }
  if (trimmed.endsWith('/api')) {
    return `${trimmed}/v1`;
  }
  return `${trimmed}/api/v1`;
}

function buildEndpointUrl(apiBaseUrl: string, resourcePath: string): string {
  const cleanBase = apiBaseUrl.replace(/\/+$/, '');
  const cleanPath = resourcePath.replace(/^\/+/, '');
  return `${cleanBase}/${cleanPath}`;
}

function decodeJwtPayload(token: string): JwtPayload {
  const parts = token.split('.');
  if (parts.length < 2) {
    return {};
  }
  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(
    base64.length + ((4 - (base64.length % 4)) % 4),
    '=',
  );
  const json = Buffer.from(padded, 'base64').toString('utf8');
  return JSON.parse(json) as JwtPayload;
}

function assertCondition(
  condition: unknown,
  message: string,
): asserts condition {
  if (!condition) {
    throw new Error(`Aserción E2E incumplida: ${message}`);
  }
}

function isSpaHtmlResponse(res: AxiosResponse): boolean {
  const contentType = String(res.headers?.['content-type'] ?? '').toLowerCase();
  const body = typeof res.data === 'string' ? res.data : '';
  return (
    contentType.includes('text/html') &&
    (body.includes('<!doctype html>') || body.includes('<div id="root">'))
  );
}

async function main(): Promise<void> {
  const rawGatewayUrl =
    process.env.API_GATEWAY_URL ??
    process.env.PUBLIC_API_URL ??
    'https://ojociudadano.dev.censei.cl';
  const apiBaseUrl = normalizeApiBaseUrl(rawGatewayUrl);

  const adminEmail = process.env.E2E_ADMIN_EMAIL ?? 'admin@ojociudadano.cl';
  const adminPassword = process.env.E2E_ADMIN_PASSWORD ?? 'Admin123!';

  const citizenEmail =
    process.env.E2E_CITIZEN_EMAIL ?? 'ciudadano.e2e@ojociudadano.cl';
  const citizenPassword = process.env.E2E_CITIZEN_PASSWORD ?? 'Ciudadano123!';
  const citizenName =
    process.env.E2E_CITIZEN_NAME ?? 'Ciudadano Prueba E2E';
  const autoRegisterCitizen =
    (process.env.E2E_AUTO_REGISTER_CITIZEN ?? 'true').toLowerCase() !== 'false';

  const reportOutputPath = path.resolve(
    process.cwd(),
    process.env.E2E_REPORT_PATH ?? '../../docs/REPORTE_EJECUCION_E2E_158.json',
  );

  const runTag = `E2E-158-${Date.now()}`;
  const executedAt = new Date().toISOString();

  const httpsAgent = new https.Agent({
    rejectUnauthorized: false,
  });

  const client: AxiosInstance = axios.create({
    timeout: 25000,
    httpsAgent,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    validateStatus: () => true,
  });

  const checks: CheckResult[] = [];

  const recordCheck = (
    step: string,
    description: string,
    method: string,
    endpointPath: string,
    fullUrl: string,
    expectedStatus: number,
    res: AxiosResponse,
    extraValidation?: () => Record<string, unknown> | void,
  ): void => {
    let passed = res.status === expectedStatus;
    let details: Record<string, unknown> | undefined;
    let validationError: Error | undefined;

    if (passed && extraValidation) {
      try {
        const result = extraValidation();
        if (result) {
          details = result;
        }
      } catch (err) {
        passed = false;
        validationError = err as Error;
      }
    }

    if (!passed && !details) {
      details = {
        requestedUrl: fullUrl,
        responseHeaders: res.headers,
        responseBody: res.data,
        validationError: validationError?.message,
      };
    }

    checks.push({
      step,
      description,
      method,
      path: endpointPath,
      url: fullUrl,
      expectedStatus,
      actualStatus: res.status,
      passed,
      details,
    });

    const icon = passed ? '✔' : '✖';
    console.log(
      `${icon} [${step}] ${method} ${endpointPath} (${fullUrl}) -> HTTP ${res.status} (esperado ${expectedStatus}): ${description}`,
    );

    if (!passed) {
      const preview =
        typeof res.data === 'string'
          ? res.data.slice(0, 200)
          : JSON.stringify(res.data);
      console.error(`  Detalle respuesta recibida: ${preview}`);
      throw new Error(
        validationError?.message ??
          `Fallo en [${step}] ${method} ${endpointPath}: se esperaba HTTP ${expectedStatus} y se recibió HTTP ${res.status}`,
      );
    }
  };

  const report: E2EReport = {
    issue: '#158',
    runTag,
    executedAt,
    environment: {
      apiGatewayBaseUrl: apiBaseUrl,
      tlsRejectUnauthorizedDisabled: true,
    },
    actors: {
      ciudadano: { id: '', email: citizenEmail, roles: [] },
      administrador: { id: '', email: adminEmail, roles: [] },
    },
    persistedData: {
      incidenteId: '',
      titulo: '',
      categoriaId: '',
      categoriaNombre: '',
      comuna: 'Temuco',
      estadoInicial: '',
      estadoConfirmado: '',
      observacionCambioEstado: '',
      historialRegistroId: '',
      historialFechaCambio: '',
      totalRegistrosHistorial: 0,
    },
    checks,
    summary: {
      totalChecks: 0,
      passedChecks: 0,
      failedChecks: 0,
      status: 'FAILED',
    },
  };

  try {
    console.log('============================================================');
    console.log(`Iniciando prueba E2E de ciclo de vida de incidente (#158)`);
    console.log(`API Gateway Base URL: ${apiBaseUrl}`);
    console.log(`Identificador de corrida: ${runTag}`);
    console.log('============================================================\n');

    // Verificación previa de enrutamiento del Ingress hacia el API Gateway
    const gatewayProbeRes = await client.get(apiBaseUrl);
    if (isSpaHtmlResponse(gatewayProbeRes)) {
      throw new Error(
        `El endpoint ${apiBaseUrl} está devolviendo el HTML estático de Nginx (web-frontend-svc) en lugar del API Gateway (api-gateway-svc). Verifique que el Ingress en Kubernetes UCT tenga aplicada la regla Single-Host 'path: /api -> api-gateway-svc:3000' (#128 / #129) o ejecute mediante port-forward con API_GATEWAY_URL=http://localhost:3000.`,
      );
    }

    const loginUrl = buildEndpointUrl(apiBaseUrl, 'auth/login');
    const registerUrl = buildEndpointUrl(apiBaseUrl, 'auth/register');

    // 1. Autenticar Ciudadano (o registrarlo si aún no existe en el entorno)
    let citizenLoginRes = await client.post(loginUrl, {
      email: citizenEmail,
      password: citizenPassword,
    });

    if (citizenLoginRes.status === 401 && autoRegisterCitizen) {
      const registerRes = await client.post(registerUrl, {
        nombre: citizenName,
        email: citizenEmail,
        password: citizenPassword,
        telefono: '+56911112222',
      });

      recordCheck(
        '01-A',
        'Registro automático de cuenta Ciudadano de prueba',
        'POST',
        '/api/v1/auth/register',
        registerUrl,
        registerRes.status === 409 ? 409 : 201,
        registerRes,
        () => ({
          email: citizenEmail,
          status: registerRes.status,
        }),
      );

      citizenLoginRes = await client.post(loginUrl, {
        email: citizenEmail,
        password: citizenPassword,
      });
    }

    let citizenToken = '';
    let citizenId = '';
    let citizenRoles: string[] = [];

    recordCheck(
      '01',
      'Autenticación real de usuario con rol CIUDADANO',
      'POST',
      '/api/v1/auth/login',
      loginUrl,
      200,
      citizenLoginRes,
      () => {
        citizenToken = citizenLoginRes.data?.access_token;
        assertCondition(
          typeof citizenToken === 'string' && citizenToken.length > 10,
          'El login de Ciudadano no devolvió un access_token válido',
        );
        const jwtPayload = decodeJwtPayload(citizenToken);
        const userObj =
          citizenLoginRes.data?.usuario ?? citizenLoginRes.data?.user;
        citizenId = userObj?.id ?? jwtPayload.sub ?? '';
        citizenRoles = userObj?.roles ?? jwtPayload.roles ?? [];

        assertCondition(
          UUID_REGEX.test(citizenId),
          `El id del Ciudadano no es un UUID válido: ${citizenId}`,
        );
        assertCondition(
          citizenRoles.includes('CIUDADANO'),
          `El usuario ciudadano no posee el rol CIUDADANO: ${JSON.stringify(citizenRoles)}`,
        );

        return {
          ciudadanoId: citizenId,
          roles: citizenRoles,
        };
      },
    );

    report.actors.ciudadano = {
      id: citizenId,
      email: citizenEmail,
      roles: citizenRoles,
    };

    // 2. Autenticar Administrador
    const adminLoginRes = await client.post(loginUrl, {
      email: adminEmail,
      password: adminPassword,
    });

    let adminToken = '';
    let adminId = '';
    let adminRoles: string[] = [];

    recordCheck(
      '02',
      'Autenticación real de usuario con rol ADMIN',
      'POST',
      '/api/v1/auth/login',
      loginUrl,
      200,
      adminLoginRes,
      () => {
        adminToken = adminLoginRes.data?.access_token;
        assertCondition(
          typeof adminToken === 'string' && adminToken.length > 10,
          'El login de Administrador no devolvió un access_token válido',
        );
        const jwtPayload = decodeJwtPayload(adminToken);
        const userObj =
          adminLoginRes.data?.usuario ?? adminLoginRes.data?.user;
        adminId = userObj?.id ?? jwtPayload.sub ?? '';
        adminRoles = userObj?.roles ?? jwtPayload.roles ?? [];

        assertCondition(
          UUID_REGEX.test(adminId),
          `El id del Administrador no es un UUID válido: ${adminId}`,
        );
        assertCondition(
          adminRoles.includes('ADMIN'),
          `El usuario administrador no posee el rol ADMIN: ${JSON.stringify(adminRoles)}`,
        );

        return {
          administradorId: adminId,
          roles: adminRoles,
        };
      },
    );

    report.actors.administrador = {
      id: adminId,
      email: adminEmail,
      roles: adminRoles,
    };

    // 3. Consultar catálogo de categorías vigentes
    const categoriasUrl = buildEndpointUrl(apiBaseUrl, 'categorias');
    const categoriasRes = await client.get(categoriasUrl, {
      headers: { Authorization: `Bearer ${citizenToken}` },
    });

    let categoriaId = '';
    let categoriaNombre = '';

    recordCheck(
      '03',
      'Consulta de categorías vigentes a través del API Gateway',
      'GET',
      '/api/v1/categorias',
      categoriasUrl,
      200,
      categoriasRes,
      () => {
        assertCondition(
          Array.isArray(categoriasRes.data) && categoriasRes.data.length > 0,
          'El catálogo de categorías está vacío o no es un arreglo',
        );
        const preferida =
          categoriasRes.data.find(
            (c: { nombre?: string }) => c.nombre === 'Alumbrado público',
          ) ?? categoriasRes.data[0];

        categoriaId = preferida.id;
        categoriaNombre = preferida.nombre;

        assertCondition(
          UUID_REGEX.test(categoriaId),
          `El id de la categoría no es un UUID válido: ${categoriaId}`,
        );

        return {
          categoriaSeleccionadaId: categoriaId,
          categoriaSeleccionadaNombre: categoriaNombre,
          totalCategoriasDisponibles: categoriasRes.data.length,
        };
      },
    );

    // 4. Crear incidente como Ciudadano
    const tituloIncidente = `Luminaria apagada sector universitario [${runTag}]`;
    const descripcionIncidente =
      'Reporte automatizado E2E (#158) para verificar creación, transición de estado e historial.';
    const incidentesUrl = buildEndpointUrl(apiBaseUrl, 'incidentes');

    const createIncidenteRes = await client.post(
      incidentesUrl,
      {
        titulo: tituloIncidente,
        descripcion: descripcionIncidente,
        categoriaId,
        usuarioReportaId: citizenId,
        ubicacion: {
          latitud: -38.7359,
          longitud: -72.5904,
          direccionReferencia: 'Av. Alemania 0211, Campus Norte',
          comuna: 'Temuco',
          region: 'La Araucanía',
          sector: 'Centro',
        },
      },
      {
        headers: { Authorization: `Bearer ${citizenToken}` },
      },
    );

    let incidenteId = '';
    let estadoInicialNombre = '';

    recordCheck(
      '04',
      'Creación de incidente por Ciudadano con categoría vigente y ubicación válida',
      'POST',
      '/api/v1/incidentes',
      incidentesUrl,
      201,
      createIncidenteRes,
      () => {
        incidenteId = createIncidenteRes.data?.id;
        estadoInicialNombre =
          createIncidenteRes.data?.estado?.nombre ?? 'Nuevo';

        assertCondition(
          UUID_REGEX.test(incidenteId),
          `La creación del incidente no devolvió un UUID válido: ${incidenteId}`,
        );
        assertCondition(
          estadoInicialNombre === 'Nuevo',
          `Se esperaba estado inicial "Nuevo" y se obtuvo "${estadoInicialNombre}"`,
        );
        assertCondition(
          createIncidenteRes.data?.usuarioReportaId === citizenId,
          `El usuarioReportaId persistido no coincide con el Ciudadano autenticado`,
        );

        return {
          incidenteId,
          titulo: createIncidenteRes.data?.titulo,
          estadoInicial: estadoInicialNombre,
          nivelPrioridad: createIncidenteRes.data?.nivelPrioridad,
        };
      },
    );

    report.persistedData.incidenteId = incidenteId;
    report.persistedData.titulo = tituloIncidente;
    report.persistedData.categoriaId = categoriaId;
    report.persistedData.categoriaNombre = categoriaNombre;
    report.persistedData.estadoInicial = estadoInicialNombre;

    const incidenteDetalleUrl = buildEndpointUrl(
      apiBaseUrl,
      `incidentes/${incidenteId}`,
    );
    const incidenteEstadoUrl = buildEndpointUrl(
      apiBaseUrl,
      `incidentes/${incidenteId}/estado`,
    );
    const incidenteHistorialUrl = buildEndpointUrl(
      apiBaseUrl,
      `incidentes/${incidenteId}/historial`,
    );

    // 5. Comprobar que un Ciudadano NO puede cambiar el estado del incidente (403 Forbidden)
    const citizenPatchEstadoRes = await client.patch(
      incidenteEstadoUrl,
      {
        estado: 'En revisión',
        observacion: 'Intento de cambio de estado por Ciudadano sin permisos',
      },
      {
        headers: { Authorization: `Bearer ${citizenToken}` },
      },
    );

    recordCheck(
      '05',
      'Rechazo 403 Forbidden cuando un Ciudadano intenta cambiar el estado del incidente',
      'PATCH',
      `/api/v1/incidentes/${incidenteId}/estado`,
      incidenteEstadoUrl,
      403,
      citizenPatchEstadoRes,
      () => ({
        statusCode: citizenPatchEstadoRes.data?.statusCode ?? 403,
        error: citizenPatchEstadoRes.data?.error ?? 'Forbidden',
      }),
    );

    const checkUnchangedAfter403Res = await client.get(incidenteDetalleUrl, {
      headers: { Authorization: `Bearer ${citizenToken}` },
    });

    recordCheck(
      '06',
      'Confirmación de que el rechazo 403 al Ciudadano no alteró el estado "Nuevo"',
      'GET',
      `/api/v1/incidentes/${incidenteId}`,
      incidenteDetalleUrl,
      200,
      checkUnchangedAfter403Res,
      () => {
        const estadoActual = checkUnchangedAfter403Res.data?.estado?.nombre;
        assertCondition(
          estadoActual === 'Nuevo',
          `El estado del incidente cambió indebidamente tras el 403: "${estadoActual}"`,
        );
        return {
          incidenteId,
          estadoConservado: estadoActual,
        };
      },
    );

    // 6. Ejecutar transición permitida (Nuevo -> En revisión) como Administrador (#116 / #117)
    const observacionAdmin = `Se inicia evaluación técnica del incidente (${runTag})`;
    const adminPatchEstadoRes = await client.patch(
      incidenteEstadoUrl,
      {
        estado: 'En revisión',
        observacion: observacionAdmin,
      },
      {
        headers: { Authorization: `Bearer ${adminToken}` },
      },
    );

    recordCheck(
      '07',
      'Cambio de estado permitido (Nuevo -> En revisión) ejecutado por Administrador con observación',
      'PATCH',
      `/api/v1/incidentes/${incidenteId}/estado`,
      incidenteEstadoUrl,
      200,
      adminPatchEstadoRes,
      () => {
        const nuevoEstado = adminPatchEstadoRes.data?.estado?.nombre;
        assertCondition(
          nuevoEstado === 'En revisión',
          `Se esperaba estado "En revisión" en la respuesta del PATCH y se obtuvo "${nuevoEstado}"`,
        );
        return {
          incidenteId,
          estadoActualizado: nuevoEstado,
          orden: adminPatchEstadoRes.data?.estado?.orden,
        };
      },
    );

    // 7. Consultar el incidente para comprobar su estado confirmado
    const getIncidenteConfirmadoRes = await client.get(incidenteDetalleUrl, {
      headers: { Authorization: `Bearer ${citizenToken}` },
    });

    recordCheck(
      '08',
      'Consulta posterior del incidente confirmando el nuevo estado persistido ("En revisión")',
      'GET',
      `/api/v1/incidentes/${incidenteId}`,
      incidenteDetalleUrl,
      200,
      getIncidenteConfirmadoRes,
      () => {
        const estadoConfirmado =
          getIncidenteConfirmadoRes.data?.estado?.nombre;
        assertCondition(
          getIncidenteConfirmadoRes.data?.id === incidenteId,
          'El id del incidente consultado no coincide',
        );
        assertCondition(
          estadoConfirmado === 'En revisión',
          `El estado persistido al consultar el incidente es "${estadoConfirmado}" en lugar de "En revisión"`,
        );
        return {
          incidenteId,
          estadoConfirmado,
        };
      },
    );

    report.persistedData.estadoConfirmado = 'En revisión';
    report.persistedData.observacionCambioEstado = observacionAdmin;

    // 8. Consultar el historial (#120) y verificar estado, actor, observación y fecha
    const getHistorialRes = await client.get(incidenteHistorialUrl, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });

    let historialCountBefore409 = 0;

    recordCheck(
      '09',
      'Consulta del historial verificando estado, actor (ADMIN), observación y fecha del cambio',
      'GET',
      `/api/v1/incidentes/${incidenteId}/historial`,
      incidenteHistorialUrl,
      200,
      getHistorialRes,
      () => {
        const registros = getHistorialRes.data;
        assertCondition(
          Array.isArray(registros) && registros.length > 0,
          'El historial devuelto no contiene registros',
        );

        historialCountBefore409 = registros.length;

        const registroEnRevision = registros.find(
          (r: { estado?: { nombre?: string }; observacion?: string }) =>
            r?.estado?.nombre === 'En revisión' &&
            r?.observacion === observacionAdmin,
        );

        assertCondition(
          Boolean(registroEnRevision),
          'No se encontró en el historial el registro correspondiente a la transición a "En revisión"',
        );
        assertCondition(
          registroEnRevision.incidenteId === incidenteId,
          `El incidenteId del historial (${registroEnRevision.incidenteId}) no coincide con ${incidenteId}`,
        );
        assertCondition(
          registroEnRevision.usuarioId === adminId,
          `El actor del historial (${registroEnRevision.usuarioId}) no coincide con el Administrador (${adminId})`,
        );
        assertCondition(
          registroEnRevision.observacion === observacionAdmin,
          `La observación del historial no coincide con la enviada`,
        );
        assertCondition(
          typeof registroEnRevision.fecha === 'string' &&
            !Number.isNaN(Date.parse(registroEnRevision.fecha)),
          `La fecha del historial no es una fecha ISO-8601 válida: ${registroEnRevision.fecha}`,
        );

        report.persistedData.historialRegistroId = registroEnRevision.id;
        report.persistedData.historialFechaCambio = registroEnRevision.fecha;
        report.persistedData.totalRegistrosHistorial = registros.length;

        return {
          historialRegistroId: registroEnRevision.id,
          estado: registroEnRevision.estado?.nombre,
          actorUsuarioId: registroEnRevision.usuarioId,
          observacion: registroEnRevision.observacion,
          fecha: registroEnRevision.fecha,
          totalRegistros: registros.length,
        };
      },
    );

    // 9. Comprobar que una transición prohibida (En revisión -> Cerrado) recibe 409 Conflict
    const observacionProhibida =
      'Intento de transición prohibida directa desde En revisión a Cerrado';
    const invalidTransitionRes = await client.patch(
      incidenteEstadoUrl,
      {
        estado: 'Cerrado',
        observacion: observacionProhibida,
      },
      {
        headers: { Authorization: `Bearer ${adminToken}` },
      },
    );

    recordCheck(
      '10',
      'Rechazo 409 Conflict ante transición prohibida (En revisión -> Cerrado)',
      'PATCH',
      `/api/v1/incidentes/${incidenteId}/estado`,
      incidenteEstadoUrl,
      409,
      invalidTransitionRes,
      () => ({
        statusCode: invalidTransitionRes.data?.statusCode ?? 409,
        message: invalidTransitionRes.data?.message,
      }),
    );

    // 10. Confirmar que el rechazo 409 no alteró el estado ni agregó un registro al historial
    const getIncidentePost409Res = await client.get(incidenteDetalleUrl, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });

    recordCheck(
      '11',
      'Confirmación de que el incidente conserva el estado "En revisión" tras el rechazo 409',
      'GET',
      `/api/v1/incidentes/${incidenteId}`,
      incidenteDetalleUrl,
      200,
      getIncidentePost409Res,
      () => {
        const estadoActual = getIncidentePost409Res.data?.estado?.nombre;
        assertCondition(
          estadoActual === 'En revisión',
          `El estado del incidente fue alterado tras una transición prohibida (409): "${estadoActual}"`,
        );
        return {
          incidenteId,
          estadoConservado: estadoActual,
        };
      },
    );

    const getHistorialPost409Res = await client.get(incidenteHistorialUrl, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });

    recordCheck(
      '12',
      'Confirmación de que la transición prohibida (409) no agregó cambios al historial',
      'GET',
      `/api/v1/incidentes/${incidenteId}/historial`,
      incidenteHistorialUrl,
      200,
      getHistorialPost409Res,
      () => {
        const registros = getHistorialPost409Res.data;
        assertCondition(
          Array.isArray(registros) &&
            registros.length === historialCountBefore409,
          `El número de registros en el historial cambió tras un 409 (antes: ${historialCountBefore409}, después: ${registros?.length})`,
        );

        const registroInvalido = registros.find(
          (r: { estado?: { nombre?: string }; observacion?: string }) =>
            r?.estado?.nombre === 'Cerrado' ||
            r?.observacion === observacionProhibida,
        );
        assertCondition(
          !registroInvalido,
          'Se encontró un registro en el historial correspondiente a la transición rechazada con 409',
        );

        return {
          totalRegistrosHistorial: registros.length,
        };
      },
    );

    report.summary = {
      totalChecks: checks.length,
      passedChecks: checks.filter((c) => c.passed).length,
      failedChecks: checks.filter((c) => !c.passed).length,
      status: 'PASSED',
    };

    fs.mkdirSync(path.dirname(reportOutputPath), { recursive: true });
    fs.writeFileSync(
      reportOutputPath,
      JSON.stringify(report, null, 2),
      'utf8',
    );

    console.log('\n============================================================');
    console.log('✔ FLUJO E2E COMPLETADO EXITOSAMENTE');
    console.log(`  Incidente creado ID : ${report.persistedData.incidenteId}`);
    console.log(
      `  Categoría           : ${report.persistedData.categoriaNombre} (${report.persistedData.categoriaId})`,
    );
    console.log(`  Ciudadano reporta ID: ${report.actors.ciudadano.id}`);
    console.log(`  Admin actor ID      : ${report.actors.administrador.id}`);
    console.log(`  Estado confirmado   : ${report.persistedData.estadoConfirmado}`);
    console.log(
      `  Historial cambio ID : ${report.persistedData.historialRegistroId}`,
    );
    console.log(
      `  Checks superados    : ${report.summary.passedChecks}/${report.summary.totalChecks}`,
    );
    console.log(`  Reporte generado en : ${reportOutputPath}`);
    console.log('============================================================');
  } catch (error) {
    const message =
      error instanceof Error ? error.message : 'Error desconocido en prueba E2E';

    report.summary = {
      totalChecks: checks.length,
      passedChecks: checks.filter((c) => c.passed).length,
      failedChecks: checks.filter((c) => !c.passed).length || 1,
      status: 'FAILED',
      errorMessage: message,
    };

    try {
      fs.mkdirSync(path.dirname(reportOutputPath), { recursive: true });
      fs.writeFileSync(
        reportOutputPath,
        JSON.stringify(report, null, 2),
        'utf8',
      );
    } catch {
      // Ignorar error secundario de escritura si la ruta no es accesible
    }

    console.error('\n============================================================');
    console.error(`✖ FLUJO E2E FALLIDO: ${message}`);
    console.error(`  Reporte de diagnóstico guardado en: ${reportOutputPath}`);
    console.error('============================================================');
    process.exit(1);
  }
}

main();