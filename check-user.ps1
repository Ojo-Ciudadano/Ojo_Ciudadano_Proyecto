$env:DATABASE_URL = "postgresql://postgres:postgres@localhost:5432/auth_usuarios"

npx prisma db execute --stdin <<'SQL'
SELECT id, nombre, email, "password_hash" FROM usuario WHERE email = 'barbara@test.cl';
SQL
