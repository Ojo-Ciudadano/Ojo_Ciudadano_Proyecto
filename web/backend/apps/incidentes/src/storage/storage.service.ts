import { Injectable, Logger } from '@nestjs/common';
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
  private readonly client: SupabaseClient;
  private readonly bucket: string;

  constructor(config: ConfigService) {
    // getOrThrow: el servicio falla al arrancar si falta configuración
    const rawUrl = config.getOrThrow<string>('SUPABASE_URL');
    const url = normalizeSupabaseUrl(rawUrl);
    const key = config.getOrThrow<string>('SUPABASE_SERVICE_ROLE_KEY');
    this.bucket = config.getOrThrow<string>('SUPABASE_BUCKET_EVIDENCES');
    this.client = createClient(url, key, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
  }

  async upload(key: string, body: Buffer, contentType: string): Promise<void> {
    try {
      const { error } = await this.client.storage
        .from(this.bucket)
        .upload(key, body, { contentType, upsert: false });
      if (error) throw error;
    } catch (error) {
      throw this.wrap('upload', error);
    }
  }

  async download(key: string): Promise<{ buffer: Buffer; contentType?: string }> {
    let result;
    try {
      result = await this.client.storage.from(this.bucket).download(key);
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
    try {
      const { error } = await this.client.storage.from(this.bucket).remove([key]);
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
    // Solo se registra el mensaje, nunca credenciales ni configuración
    const message = error instanceof Error ? error.message : 'error desconocido';
    this.logger.error(`Storage ${operation} falló: ${message}`);
    return new StorageOperationError(operation, message);
  }
}
