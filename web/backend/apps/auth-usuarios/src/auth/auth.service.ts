import { ConflictException, Injectable } from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { UsuariosService } from '../usuarios/usuarios.service';
import { CreateUsuarioDto } from '../usuarios/dto/create-usuario.dto';

const SALT_ROUNDS = 10;

@Injectable()
export class AuthService {
  constructor(private readonly usuariosService: UsuariosService) {}

  async register(dto: CreateUsuarioDto) {
    const existente = await this.usuariosService.findByEmail(dto.email);
    if (existente) {
      throw new ConflictException('Ya existe una cuenta con ese correo');
    }

    const passwordHash = await bcrypt.hash(dto.password, SALT_ROUNDS);

    const usuario = await this.usuariosService.create({
      nombre: dto.nombre,
      email: dto.email,
      passwordHash,
      telefono: dto.telefono,
    });

    const { passwordHash: _omit, ...usuarioSinPassword } = usuario;
    return usuarioSinPassword;
  }
}