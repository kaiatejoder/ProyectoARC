# 📘 Manual de Usuario - ProyectoARC

## Tabla de Contenidos
1. [Introducción](#introducción)
2. [Requisitos del Sistema](#requisitos-del-sistema)
3. [Instalación y Compilación](#instalación-y-compilación)
4. [Arquitectura del Sistema](#arquitectura-del-sistema)
5. [Guía de Ejecución](#guía-de-ejecución)
6. [Parámetros de Configuración](#parámetros-de-configuración)
7. [Interpretación de Resultados](#interpretación-de-resultados)
8. [Resolución de Problemas](#resolución-de-problemas)
9. [Ejemplos de Uso](#ejemplos-de-uso)

---

## Introducción

**ProyectoARC** es una aplicación de simulación distribuida que modela la comunicación en red entre múltiples clientes organizados en grupos. El sistema implementa un protocolo híbrido **TCP/UDP**:

- **TCP**: Para el registro inicial fiable de clientes
- **UDP**: Para la simulación de alta velocidad de intercambio de coordenadas

### Objetivo Principal
Medir y analizar la **latencia**, **throughput** y **eficiencia** de un sistema cliente-servidor distribuido bajo diferentes cargas y configuraciones.

### Casos de Uso
- Simulación de redes de sensores distribuidos
- Análisis de rendimiento de comunicación UDP vs TCP
- Evaluación de sincronización entre pares (peers) en grupos
- Generación de métricas de desempeño para análisis estadístico

---

## Requisitos del Sistema

### Software
- **Java Development Kit (JDK)** versión 11 o superior
- **Maven** 3.6+ (para compilación)
- **Sistema operativo**: Windows, Linux o macOS

### Hardware Recomendado
- **Procesador**: Mínimo 2 núcleos (más núcleos = mejor rendimiento paralelo)
- **Memoria RAM**: Mínimo 2GB (para simulaciones grandes)
- **Espacio en disco**: 100MB para el proyecto completo

### Validación de Requisitos
```bash
# Verificar Java
java -version

# Verificar Maven
mvn -version
```

---

## Instalación y Compilación

### Paso 1: Clonar o Descargar el Proyecto
```bash
cd tu_directorio
git clone https://github.com/kaiatejoder/ProyectoARC.git
cd ProyectoARC
```

### Paso 2: Compilar con Maven
```bash
cd ProyectoARC
mvn clean compile
```

**Salida esperada:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXs
```

### Paso 3: Empaquetar (Opcional)
```bash
mvn package
```

---

## Arquitectura del Sistema

### Flujo General de Ejecución

```
┌─────────────────────────────────────────────────────────┐
│          FASE 1: REGISTRO (TCP)                         │
│  Servidor escucha en puerto 10578                       │
│  Clientes se conectan y reciben ID + Grupo asignados   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│        FASE 2: SIMULACIÓN (UDP Paralelo)               │
│  - Servidor envía señal de inicio                       │
│  - Clientes intercambian coordenadas en paralelo        │
│  - Se miden tiempos de respuesta y latencias            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│      FASE 3: CIERRE (Sincronización de Grupos)         │
│  - Clientes reportan tiempos finales                    │
│  - Servidor valida que todos en grupo terminaron       │
│  - Envía señal de cierre (GROUP_DONE)                   │
└─────────────────────────────────────────────────────────┘
```

### Componentes Principales

| Componente | Rol | Responsabilidad |
|-----------|-----|-----------------|
| **Servidor.java** | Coordinador central | Registra clientes, retransmite mensajes, sincroniza grupos |
| **Cliente.java** | Lanzador | Crea múltiples instancias de Persona y recopila estadísticas |
| **Persona.java** | Cliente inteligente | Simula un nodo que intercambia coordenadas con vecinos |
| **Mensaje.java** | Protocolo | Define formatos de mensajes (TCP/UDP) |
| **CSVWriter.java** | Exportador de datos | Genera archivos CSV con métricas |

### Modelo de Comunicación

#### Tipos de Mensaje
```
INICIAR_SIMULACION      → Servidor → Clientes (Señal de inicio)
COMPARTIR_COORDENADAS   → Cliente → Servidor → Vecinos (Carga principal)
ACK                     → Vecino → Cliente (Confirmación)
TIEMPOS_SIMULACION      → Cliente → Servidor (Resultado final)
GROUP_DONE              → Servidor → Clientes (Señal de cierre)
```

#### Modelo de Grupos
- Los N clientes se dividen automáticamente en **N/V grupos**
- Cada grupo tiene exactamente **V miembros** (vecinos)
- Los clientes en el mismo grupo intercambian coordenadas entre sí
- La sincronización se hace por grupo (no global)

**Ejemplo:**
```
N=12, V=4 → 3 grupos de 4 clientes cada uno

Grupo 0: Clientes [0,1,2,3]
Grupo 1: Clientes [4,5,6,7]
Grupo 2: Clientes [8,9,10,11]
```

---

## Guía de Ejecución

### Ejecución Paso a Paso

#### **Paso 1: Iniciar el Servidor**

```bash
cd ProyectoARC/target/classes
java com.g13.ProyectoARC_2025_11_23.Servidor
```

**El servidor pedirá:**
```
--- CONFIGURACIÓN DEL SERVIDOR HÍBRIDO (TCP+UDP) ---
Introduce N (Total clientes): 
Introduce V (Vecinos por grupo): 
Introduce S (Iteraciones): 
```

**Ejemplo:**
```
Introduce N (Total clientes): 20
Introduce V (Vecinos por grupo): 5
Introduce S (Iteraciones): 100
```

**Salida esperada:**
```
--- FASE 1: REGISTRO (TCP) ---
Esperando a 20 clientes en puerto TCP 10578...
--- FASE 2: SIMULACIÓN (UDP MULTI-HILO) ---
Señal de inicio enviada. Procesando mensajes en paralelo...
```

#### **Paso 2: Iniciar los Clientes (En otra terminal)**

```bash
cd ProyectoARC/target/classes
java com.g13.ProyectoARC_2025_11_23.Cliente
```

**El cliente pedirá:**
```
--- CONFIGURACIÓN DEL CLIENTE ---
Introduce la IP (ej. 127.0.0.1): 
Introduce el puerto (ej. 10578): 
Introduce N (Total Clientes): 
Introduce V (Vecinos): 
Introduce S (Iteraciones): 
```

**Ejemplo (DEBE coincidir con el servidor):**
```
Introduce la IP (ej. 127.0.0.1): 127.0.0.1
Introduce el puerto (ej. 10578): 10578
Introduce N (Total Clientes): 20
Introduce V (Vecinos): 5
Introduce S (Iteraciones): 100
```

#### **Paso 3: Esperar Resultados**

El cliente esperará a que se complete la simulación y mostrará:

```
==================================================
       REPORTE FINAL DE LA SIMULACIÓN
==================================================
Clientes Totales:       20
Clientes Exitosos:      20 (100.00%)
Clientes Fallidos:      0
--------------------------------------------------
Tiempo Medio Global:    45.3200 ms
Total Timeouts (UDP):   0
Throughput:             2847563.45 bits/s
==================================================
 ESTADO: ÉXITO TOTAL
==================================================
```

---

## Parámetros de Configuración

### Parámetros Críticos

| Parámetro | Símbolo | Significado | Rango Recomendado |
|-----------|---------|-------------|------------------|
| **Total Clientes** | N | Número de clientes que participan en la simulación | 1-1000 |
| **Vecinos** | V | Tamaño de cada grupo (debe dividir a N) | 2-50 |
| **Iteraciones** | S | Ciclos de intercambio de coordenadas por cliente | 10-10000 |

### Restricciones Importantes

⚠️ **CRÍTICO**: N debe ser divisible exactamente por V
```
✅ N=20, V=5  (20÷5=4 grupos) → VÁLIDO
✅ N=100, V=10 (100÷10=10 grupos) → VÁLIDO
❌ N=20, V=7  (no es múltiplo) → ERROR
```

### Combinaciones Recomendadas

#### Para Pruebas Rápidas
```
N=10, V=5, S=10
Tiempo estimado: < 1 segundo
```

#### Para Pruebas Medias
```
N=100, V=10, S=100
Tiempo estimado: 5-10 segundos
```

#### Para Pruebas Exhaustivas
```
N=1000, V=20, S=1000
Tiempo estimado: 30-60 segundos
(Requiere máquina potente)
```

---

## Interpretación de Resultados

### Archivo de Salida: Reporte en Consola

#### Métricas Principales

```
Clientes Exitosos: 20 (100.00%)
```
- **Significado**: Porcentaje de clientes que completaron exitosamente
- **Objetivo**: 100% (ningún cliente debe fallar)

```
Tiempo Medio Global: 45.3200 ms
```
- **Significado**: Latencia promedio para completar 1 ciclo de intercambio
- **Objetivo**: Más bajo es mejor (depende de hardware)
- **Fórmula**: Promedio de (tiempo_fin - tiempo_inicio) de todos los ciclos

```
Total Timeouts (UDP): 0
```
- **Significado**: Mensajes UDP no confirmados en 10 segundos
- **Objetivo**: 0 (sin pérdidas)
- **Causa si > 0**: Red congestionada, servidor lento, firewall

```
Throughput: 2847563.45 bits/s
```
- **Significado**: Cantidad de datos procesados por segundo
- **Objetivo**: Más alto es mejor
- **Fórmula**: (mensajes_totales × 1024 bits) / tiempo_total

### Archivos CSV Generados

Los datos se guardan en `data/` para análisis adicional:

#### `LatenciaporClientes.csv`
```
NumeroClientes,LatenciaMediaMs
10,23.45
20,45.32
50,102.15
100,198.76
```
**Uso**: Analizar cómo escala la latencia con el número de clientes

#### `LatenciaporGVecinos.csv`
```
NumeroGrupos,LatenciaMediaMs
10,198.76
```
**Uso**: Comparar latencia entre diferentes tamaños de grupos

#### `ThroughPutPorClientes.csv`
```
NumeroClientes,BitsPorSegundo
10,1234567.89
20,2847563.45
```
**Uso**: Analizar rendimiento del sistema según carga

---

## Resolución de Problemas

### Problema 1: "Puerto 10578 en uso"
**Síntomas:**
```
Exception: Address already in use: bind
```

**Soluciones:**
1. Esperar 60 segundos (TIME_WAIT del SO)
2. Cambiar puerto: Ambos modificar el código
3. Usar `netstat` para encontrar el proceso:
```bash
netstat -ano | find "10578"
```

### Problema 2: "Connection refused" en Cliente
**Síntomas:**
```
Exception: Connection refused
```

**Soluciones:**
1. Verificar que el servidor está ejecutándose
2. Verificar que la IP es correcta (usar `127.0.0.1` para localhost)
3. Verificar que no hay firewall bloqueando el puerto

### Problema 3: Clientes no registrados
**Síntomas:**
```
Esperando a 20 clientes... [bloqueo indefinido]
```

**Soluciones:**
1. Verificar que N en cliente coincida con N en servidor
2. Revisar que el cliente no genera excepción antes de conectarse
3. Aumentar timeout: Revisar socket timeout en código

### Problema 4: Timeouts UDP (Total Timeouts > 0)
**Síntomas:**
```
Total Timeouts (UDP): 5
```

**Soluciones:**
1. Aumentar S_iteraciones para que el cliente espere más
2. Reducir N para menos carga
3. Aumentar V para grupos más pequeños
4. Aumentar timeout en `Persona.java` línea ~93

### Problema 5: Diferencia en resultados entre ejecuciones
**Esto es NORMAL**: UDP no garantiza orden ni tiempo determinista. Los tiempos variarán por:
- Carga del sistema operativo
- Variabilidad de red (aunque sea local)
- Planificación de hilos

---

## Ejemplos de Uso

### Ejemplo 1: Simulación Pequeña (Testing Rápido)

**Terminal 1 - Servidor:**
```bash
java com.g13.ProyectoARC_2025_11_23.Servidor
```
Ingresar: `N=10`, `V=5`, `S=20`

**Terminal 2 - Cliente:**
```bash
java com.g13.ProyectoARC_2025_11_23.Cliente
```
Ingresar: `IP=127.0.0.1`, `Puerto=10578`, `N=10`, `V=5`, `S=20`

**Resultado esperado:**
```
Clientes Exitosos: 10 (100.00%)
Tiempo Medio Global: 15.2345 ms
```

---

### Ejemplo 2: Simulación con Grupos Grandes

**Terminal 1 - Servidor:**
```bash
java com.g13.ProyectoARC_2025_11_23.Servidor
```
Ingresar: `N=100`, `V=20`, `S=500`

**Terminal 2 - Cliente:**
```bash
java com.g13.ProyectoARC_2025_11_23.Cliente
```
Ingresar: Mismos parámetros que servidor

**Resultado esperado:**
```
Clientes Exitosos: 100 (100.00%)
Tiempo Medio Global: 125.7832 ms
Throughput: 4256123.45 bits/s
```

---

### Ejemplo 3: Análisis Comparativo

Ejecutar varias veces con diferentes valores y comparar:

```
Configuración 1: N=50, V=10, S=100
Resultado: Tiempo medio = 52.34 ms

Configuración 2: N=50, V=5, S=100
Resultado: Tiempo medio = 25.12 ms (✓ Grupos más pequeños = menos latencia)

Configuración 3: N=50, V=25, S=100
Resultado: Tiempo medio = 98.76 ms (⚠ Grupos más grandes = mayor latencia)
```

---

## Notas Importantes para Usuarios Avanzados

### Optimizaciones

1. **Para máximo throughput:**
   - Aumentar tamaño de thread pool en `Servidor.java` línea ~28
   - Aumentar N con S bajo (ej: N=1000, S=10)

2. **Para mínima latencia:**
   - Usar grupos pequeños (V=2-5)
   - Ejecutar en máquina dedicada sin otras aplicaciones
   - Usar ethernet (no WiFi)

3. **Para investigación:**
   - Modificar timeout UDP en `Persona.java` línea ~93
   - Aumentar verbosidad: Descomentar logs en servidor/cliente
   - Exportar datos CSV para análisis con Pandas/R

### Limitaciones Conocidas

- **UDP sin garantía**: Los mensajes pueden perderse (pero rara vez en red local)
- **Single-server**: No escalable a múltiples servidores
- **Computadora local**: La simulación es más rápida en red local que internet
- **Máximo recomendado**: N=1000 clientes en máquina de 4GB RAM

---

## Soporte y Contacto

Para reportar bugs o solicitar mejoras:
- GitHub: https://github.com/kaiatejoder/ProyectoARC
- Issues: https://github.com/kaiatejoder/ProyectoARC/issues

---

**Versión del Manual**: 1.0  
**Última actualización**: Diciembre 2025  
**Aplicación**: ProyectoARC 2025.11.23
