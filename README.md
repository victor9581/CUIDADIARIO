# 🏥 CuidaDiario - Ecosistema de Monitoreo Clínico

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![PHP](https://img.shields.io/badge/php-%23777BB4.svg?style=for-the-badge&logo=php&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%2300f.svg?style=for-the-badge&logo=mysql&logoColor=white)

**CuidaDiario** es un sistema informático bidireccional diseñado para el control y seguimiento de tratamientos médicos en el hogar. Conecta a pacientes con sus cuidadores (tutores) en tiempo real mediante la integración de una aplicación móvil nativa y un Centro de Monitoreo Web.

## ✨ Características Principales

### 📱 Aplicación Móvil (Perfil Paciente)
- **Gestión de Protocolos:** Registro de medicamentos con dosis, intervalos y fechas.
- **Alarmas Precisas:** Notificaciones locales orquestadas con `AlarmManager` y persistencia en `SQLite/Room`.
- **Auditoría GPS:** Captura exacta de latitud y longitud al confirmar una toma de medicamento.
- **Tolerancia a Fallos:** Escudo de validación contra persistencia cruzada y alarmas huérfanas.
- **Alerta de Emergencia:** Recepción de notificaciones prioritarias que encienden la pantalla del dispositivo bloqueado mediante `Full Screen Intent` y `Foreground Services`.

### 💻 Dashboard Web (Perfil Tutor)
- **Aislamiento de Sesión:** Acceso seguro a través del Código Familiar (`CD-XXXX`) generado por el paciente.
- **Mapa de Telemetría:** Renderización satelital interactiva con *Leaflet.js* para visualizar las coordenadas de cada toma confirmada.
- **Bitácora en Tiempo Real:** Interfaz analítica en *Bootstrap 5* para auditar el cumplimiento del tratamiento.
- **Disparador Remoto:** Capacidad de enviar una alerta urgente directamente al teléfono físico del paciente.

## 🏗️ Arquitectura del Sistema

El proyecto opera bajo una arquitectura **Cliente-Servidor** desacoplada:
1. **Frontend Móvil:** Desarrollado en Kotlin nativo (Mobile-First).
2. **Backend & API REST:** Desarrollado en PHP 8 nativo, gestionando peticiones HTTP (GET/POST) asíncronas.
3. **Base de Datos:** MySQL / MariaDB conectada a través de *PDO* para la prevención de inyecciones SQL.
4. **Infraestructura:** Despliegue en la nube a través de *Alwaysdata*.

## 🚀 Instalación y Configuración

### Requisitos Previos
- **Android Studio** (Koala o superior) para compilar la aplicación móvil.
- Un dispositivo físico Android o emulador con Google Play Services (para geolocalización).
- Servidor web con soporte para PHP 8 y MySQL (ej. XAMPP, Alwaysdata).

### Configuración del Backend
1. Importar el archivo `cuidadiario_bd.sql` en tu gestor de base de datos MySQL.
2. Subir los scripts PHP de la carpeta `api_cuidadiario` al directorio público del servidor web.
3. Actualizar las credenciales de conexión en el archivo `conexion.php`.

### Configuración de la App Android
1. Abrir el proyecto en Android Studio.
2. Navegar a la clase `SyncManager.kt` (o donde se centralicen las peticiones HTTP) y actualizar la URL base apuntando a tu servidor web.
3. Compilar y ejecutar en un dispositivo físico.

## 👨‍💻 Autor

**Víctor Manuel Aguilar López**  
Estudiante de Licenciatura en Administración de Sistemas Informáticos  
Universidad Regional de Guatemala  
*Proyecto Final - Desarrollo de Aplicaciones Móviles*
