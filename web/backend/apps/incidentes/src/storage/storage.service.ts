import { Injectable, Logger, ServiceUnavailableException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createClient, SupabaseClient } from '@supabase/supabase-js';

export class StorageObjectNotFoundError extends Error {
  constructor(public readonly key: string) {
    super(`Objeto no encontrado en el storage: ${key}`);
    this.name = 'StorageObjectNotFoundError';
  }
}

export class StorageOperationError extends Error {
  constructor(public readonly operation: string, message: string) {
    super(`Fallo de storage en ${operation}: ${message}`);
    this.name = 'StorageOperationError';
  }
}

export function normalizeSupabaseUrl(rawUrl: string): string {
  return rawUrl
    .trim()
    .replace(/\/rest\/v1\/?$/i, '')
    .replace(/\/+$/, '');
}

@Injectable()
export class StorageService {
  private readonly logger = new Logger(StorageService.name);
  private readonly client: SupabaseClient | null = null;
  private readonly bucket: string | null = null;

  constructor(config: ConfigService) {
    try {
      const rawUrl = config.get<string>('SUPABASE_URL');
      const key = config.get<string>('SUPABASE_SERVICE_ROLE_KEY');
      const bucket = config.get<string>('SUPABASE_BUCKET_EVIDENCES');

      if (!rawUrl || !key || !bucket) {
        this.logger.warn('Faltan variables de entorno de Supabase. El servicio de Storage iniciará deshabilitado.');
        return;
      }

      const url = normalizeSupabaseUrl(rawUrl);
      this.client = createClient(url, key, {
        auth: { persistSession: false, autoRefreshToken: false },
      });
      this.bucket = bucket;
      
      this.logger.log('StorageService inicializado correctamente.');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Error desconocido';
      this.logger.warn(`Fallo al inicializar cliente Supabase: ${message}. El servicio de Storage estará deshabilitado.`);
    }
  }

  private requireClient(): { client: SupabaseClient; bucket: string } {
    if (!this.client || !this.bucket) {
      throw new ServiceUnavailableException('El servicio de almacenamiento de evidencias no está disponible temporalmente');
    }
    return { client: this.client, bucket: this.bucket };
  }

  async upload(key: string, body: Buffer, contentType: string): Promise<void> {
    const { client, bucket } = this.requireClient();
    try {
      const { error } = await client.storage
        .from(bucket)
        .upload(key, body, { contentType, upsert: false });
      if (error) throw error;
    } catch (error) {
      throw this.wrap('upload', error);
    }
  }

  async download(key: string): Promise<{ buffer: Buffer; contentType?: string }> {
    const { client, bucket } = this.requireClient();
    let result;
    try {
      result = await client.storage.from(bucket).download(key);
    } catch (error) {
      throw this.wrap('download', error);
    }

    if (result.error) {
      if (this.isNotFound(result.error)) throw new StorageObjectNotFoundError(key);
      throw this.wrap('download', result.error);
    }

    try {
      const buffer = Buffer.from(await result.data.arrayBuffer());
      return { buffer, contentType: result.data.type || undefined };
    } catch (error) {
      throw this.wrap('download', error);
    }
  }

  async remove(key: string): Promise<void> {
    const { client, bucket } = this.requireClient();
    try {
      const { error } = await client.storage.from(bucket).remove([key]);
      if (error) throw error;
    } catch (error) {
      throw this.wrap('remove', error);
    }
  }

  private isNotFound(error: unknown): boolean {
    const e = error as { status?: number; statusCode?: string | number; message?: string };
    return e.status === 404 || String(e.statusCode) === '404' || /not found/i.test(e.message ?? '');
  }

  private wrap(operation: string, error: unknown): StorageOperationError {
    const message = error instanceof Error ? error.message : 'error desconocido';
    this.logger.error(`Storage ${operation} falló: ${message}`);
    return new StorageOperationError(operation, message);
  }
}
