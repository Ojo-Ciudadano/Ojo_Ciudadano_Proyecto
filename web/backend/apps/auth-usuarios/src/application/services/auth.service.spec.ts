import bcrypt from 'bcryptjs';
import { describe, it, expect, beforeEach, jest } from '@jest/globals';
import { UnauthorizedException } from '@nestjs/common';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let prisma: any;
  let jwtService: any;
  let usuariosService: any;

  const usuario = {
    id: '550e8400-e29b-41d4-a716-446655440001',
    nombre: 'Admin',
    email: 'admin@ojociudadano.cl',
    passwordHash: bcrypt.hashSync('password-seguro', 4),
    roles: [{ rol: { nombre: 'ADMIN' } }],
  };

  beforeEach(async () => {
    prisma = {
      usuario: {
        findUnique: jest.fn<any>(),
        create: jest.fn<any>(),
      },
    };

   jwtService = {
  signAsync: jest.fn().mockImplementation(() => Promise.resolve('jwt-firmado')),
};

    usuariosService = {
      create: jest.fn<any>(),
    };

    service = new AuthService(prisma, jwtService, usuariosService);
  });

  describe('login', () => {
    it('emite un token para credenciales correctas', async () => {
      prisma.usuario.findUnique.mockResolvedValue(usuario);

      const result = await service.login({
        email: usuario.email,
        password: 'password-seguro',
      });

     expect(result).toEqual({
  access_token: 'jwt-firmado',
  token_type: 'Bearer',
  usuario: {
    id: usuario.id,
    nombre: usuario.nombre,
    email: usuario.email,
    roles: ['ADMIN'],
  },
});
    });

    it('rechaza un correo inexistente', async () => {
      prisma.usuario.findUnique.mockResolvedValue(null);

      await expect(
        service.login({ email: 'nadie@example.cl', password: 'password-seguro' }),
      ).rejects.toBeInstanceOf(UnauthorizedException);
    });

    it('rechaza una contraseña incorrecta', async () => {
      prisma.usuario.findUnique.mockResolvedValue(usuario);

      await expect(
        service.login({ email: usuario.email, password: 'incorrecta' }),
      ).rejects.toBeInstanceOf(UnauthorizedException);
    });
  });
});