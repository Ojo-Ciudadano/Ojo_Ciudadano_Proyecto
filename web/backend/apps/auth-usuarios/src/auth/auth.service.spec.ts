import * as bcrypt from 'bcrypt';
import { JwtService } from '@nestjs/jwt';
import { UnauthorizedException } from '@nestjs/common';
import { AuthService } from './auth.service';
import { UsuariosService } from '../usuarios/usuarios.service';

describe('AuthService.login', () => {
  const usuario = {
    id: '550e8400-e29b-41d4-a716-446655440001',
    nombre: 'Administrador',
    email: 'admin@ojociudadano.cl',
    passwordHash: bcrypt.hashSync('password-seguro', 4),
  };
  const usuariosService = {
    findByEmail: jest.fn(),
  };
  const jwtService = {
    signAsync: jest.fn().mockResolvedValue('jwt-firmado'),
  };
  const service = new AuthService(
    usuariosService as unknown as UsuariosService,
    jwtService as unknown as JwtService,
  );

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('emite un token para credenciales correctas', async () => {
    usuariosService.findByEmail.mockResolvedValue(usuario);

    const resultado = await service.login({ email: usuario.email, password: 'password-seguro' });

    expect(resultado).toMatchObject({
      access_token: 'jwt-firmado',
      token_type: 'Bearer',
      usuario: {
        id: usuario.id,
        nombre: usuario.nombre,
        email: usuario.email,
        roles: ['CIUDADANO'],
      },
      user: {
        id: usuario.id,
        nombre: usuario.nombre,
        email: usuario.email,
        roles: ['CIUDADANO'],
      },
    });

    expect(jwtService.signAsync).toHaveBeenCalledWith({
      sub: usuario.id,
      email: usuario.email,
      roles: ['CIUDADANO'],
    });
  });

  it('rechaza un correo inexistente o una contraseña incorrecta', async () => {
    usuariosService.findByEmail.mockResolvedValueOnce(null).mockResolvedValueOnce(usuario);

    await expect(service.login({ email: 'nadie@example.cl', password: 'password-seguro' }))
      .rejects.toBeInstanceOf(UnauthorizedException);
    await expect(service.login({ email: usuario.email, password: 'incorrecta' }))
      .rejects.toBeInstanceOf(UnauthorizedException);
    expect(jwtService.signAsync).not.toHaveBeenCalled();
  });
});
