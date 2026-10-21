-- AlterTable: se renombran las columnas existentes para conservar la metadata ya persistida
ALTER TABLE "evidencia" RENAME COLUMN "url_archivo" TO "clave_archivo";
ALTER TABLE "evidencia" RENAME COLUMN "tipo" TO "tipo_mime";
ALTER TABLE "evidencia" RENAME COLUMN "fecha_subida" TO "fecha_creacion";

-- AlterTable: tamaño del archivo en bytes (se crea con valor por defecto y luego se retira)
ALTER TABLE "evidencia" ADD COLUMN "tamano_archivo" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "evidencia" ALTER COLUMN "tamano_archivo" DROP DEFAULT;

-- CreateIndex
CREATE INDEX "evidencia_incidente_id_idx" ON "evidencia"("incidente_id");
