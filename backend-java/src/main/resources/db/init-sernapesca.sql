-- init-sernapesca.sql
-- Inicialización para SQL Server: crea base, tablas Laboratorio y Usuario, y datos de prueba

IF DB_ID(N'sernapesca') IS NULL
BEGIN
    CREATE DATABASE sernapesca;
END
GO

USE sernapesca;
GO

-- Tabla Laboratorio
IF OBJECT_ID(N'dbo.Laboratorio', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Laboratorio (
    id_laboratorio INT IDENTITY(1,1) PRIMARY KEY,
    codigo NVARCHAR(20) NOT NULL UNIQUE,
    nombre NVARCHAR(200) NOT NULL,
    tipo_laboratorio NVARCHAR(100) NULL,
    activo BIT NOT NULL DEFAULT(1)
);
END
GO

-- Tabla Usuario
IF OBJECT_ID(N'dbo.Usuario', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Usuario (
    id_usuario INT IDENTITY(1,1) PRIMARY KEY,
    rut NVARCHAR(20) NOT NULL UNIQUE,
    nombre_completo NVARCHAR(200) NOT NULL,
    email NVARCHAR(150) NULL,
    rol NVARCHAR(20) NOT NULL CHECK (rol IN ('LABORATORIO','FUNCIONARIO','ADMINISTRADOR')),
    activo BIT NOT NULL DEFAULT(1),
    id_laboratorio INT NULL,
    CONSTRAINT FK_Usuario_Laboratorio FOREIGN KEY (id_laboratorio) REFERENCES dbo.Laboratorio(id_laboratorio)
);
END
GO

-- Datos de prueba: laboratorio y usuario admin
SET IDENTITY_INSERT dbo.Laboratorio ON;
IF NOT EXISTS (SELECT 1 FROM dbo.Laboratorio WHERE codigo = 'LAB01')
BEGIN
    INSERT INTO dbo.Laboratorio (id_laboratorio, codigo, nombre, tipo_laboratorio, activo) VALUES (1, 'LAB01', 'Laboratorio Test', 'RRA', 1);
END
SET IDENTITY_INSERT dbo.Laboratorio OFF;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Usuario WHERE rut = '12345678-9')
BEGIN
    INSERT INTO dbo.Usuario (rut, nombre_completo, email, rol, activo, id_laboratorio)
    VALUES ('12345678-9', 'Admin Test', 'admin@example.com', 'ADMINISTRADOR', 1, NULL);
END
GO

-- Nota: en producción manejar secrets y contraseñas de forma segura.
