import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  MAX_FOTOGRAFIA_BYTES,
  validarFotografia,
} from '../types/fotografiaReporte'

function archivo(
  tipo: string,
  bytes: number,
  nombre = 'fotografia.jpg',
): File {
  return new File([new Uint8Array(bytes)], nombre, {
    type: tipo,
  })
}

describe('Validación de fotografías del reporte (#137)', () => {
  it('acepta fotografías JPEG, PNG y WebP', () => {
    for (const tipo of [
      'image/jpeg',
      'image/png',
      'image/webp',
    ]) {
      assert.equal(validarFotografia(archivo(tipo, 100)), null)
    }
  })

  it('acepta una fotografía de exactamente 10 MB', () => {
    assert.equal(
      validarFotografia(
        archivo('image/jpeg', MAX_FOTOGRAFIA_BYTES),
      ),
      null,
    )
  })

  it('rechaza una fotografía que supera 10 MB por un byte', () => {
    assert.match(
      validarFotografia(
        archivo('image/jpeg', MAX_FOTOGRAFIA_BYTES + 1),
      ) ?? '',
      /10 MB/,
    )
  })

  it('rechaza un archivo vacío', () => {
    assert.match(
      validarFotografia(archivo('image/png', 0)) ?? '',
      /vacío/,
    )
  })

  it('rechaza GIF, PDF y archivos sin tipo reconocido', () => {
    for (const tipo of [
      'image/gif',
      'application/pdf',
      '',
    ]) {
      assert.match(
        validarFotografia(archivo(tipo, 100)) ?? '',
        /Formato no permitido/,
      )
    }
  })

  it('no acepta un PDF solo porque su nombre termine en jpg', () => {
    assert.match(
      validarFotografia(
        archivo('application/pdf', 100, 'documento.jpg'),
      ) ?? '',
      /Formato no permitido/,
    )
  })
})