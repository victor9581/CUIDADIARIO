# CuidaDiario - Ecosistema de Monitoreo Clinico

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![PHP](https://img.shields.io/badge/php-%23777BB4.svg?style=for-the-badge&logo=php&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%2300f.svg?style=for-the-badge&logo=mysql&logoColor=white)

CuidaDiario es un sistema informatico bidireccional diseñado para el control y seguimiento de tratamientos medicos en el hogar. Conecta a pacientes con sus cuidadores (tutores) en tiempo real mediante la integracion de una aplicacion movil nativa y un Centro de Monitoreo Web.

## Caracteristicas Principales

### Aplicacion Movil (Perfil Paciente)
- **Gestion de Protocolos:** Registro de medicamentos con dosis, intervalos y fechas.
- **Alarmas Precisas:** Notificaciones locales orquestadas con AlarmManager y persistencia en SQLite/Room.
- **Auditoria GPS:** Captura exacta de latitud y longitud al confirmar una toma de medicamento.
- **Tolerancia a Fallos:** Escudo de validacion contra persistencia cruzada y alarmas huerfanas.
- **Alerta de Emergencia:** Recepcion de notificaciones prioritarias que encienden la pantalla del dispositivo bloqueado mediante Full Screen Intent y Foreground Services.

### Dashboard Web (Perfil Tutor)
- **Aislamiento de Sesion:** Acceso seguro a traves del Codigo Familiar (CD-XXXX) generado por el paciente.
- **Mapa de Telemetria:** Renderizacion satelital interactiva con Leaflet.js para visualizar las coordenadas de cada toma confirmada.
- **Bitacora en Tiempo Real:** Interfaz analitica en Bootstrap 5 para auditar el cumplimiento del tratamiento.
- **Disparador Remoto:** Capacidad de enviar una alerta urgente directamente al telefono fisico del paciente.

## Arquitectura del Sistema

El proyecto opera bajo una arquitectura Cliente-Servidor desacoplada:
1. **Frontend Movil:** Desarrollado en Kotlin nativo (Mobile-First).
2. **Backend y API REST:** Desarrollado en PHP 8 nativo, gestionando peticiones HTTP (GET/POST) asincronas.
3. **Base de Datos:** MySQL / MariaDB conectada a traves de PDO para la prevencion de inyecciones SQL.
4. **Infraestructura:** Despliegue en la nube a traves de Alwaysdata.

## Instalacion y Configuracion

### Requisitos Previos
- Android Studio (Koala o superior) para compilar la aplicacion movil.
- Un dispositivo fisico Android o emulador con Google Play Services (para geolocalizacion).
- Servidor web con soporte para PHP 8 y MySQL (ej. XAMPP, Alwaysdata).

### Configuracion del Backend
1. Importar el archivo `cuidadiario_bd.sql` en tu gestor de base de datos MySQL.
2. Subir los scripts PHP de la carpeta `api_cuidadiario` al directorio publico del servidor web.
3. Actualizar las credenciales de conexion en el archivo `conexion.php`.

### Configuracion de la App Android
1. Abrir el proyecto en Android Studio.
2. Navegar a la clase `SyncManager.kt` (o donde se centralicen las peticiones HTTP) y actualizar la URL base apuntando a tu servidor web.
3. Compilar y ejecutar en un dispositivo fisico.

## Autor

**Victor Manuel Aguilar Lopez**  
Estudiante de Licenciatura en Administracion de Sistemas Informaticos  
Universidad Regional de Guatemala  
Proyecto Final - Desarrollo de Aplicaciones Moviles
