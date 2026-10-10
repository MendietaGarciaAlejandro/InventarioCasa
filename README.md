# Inventario de casa

App Android para llevar la cuenta de lo que hay en casa. Nació de un problema concreto: comprar algo que ya teníamos porque nadie se acordaba.

Comparte base de datos (Supabase) con la app de la lista de la compra y con su web, pero es una app aparte a propósito: la de la compra la usa mi padre y quería que siguiera igual de simple.

## Qué hace

- Lista de artículos por categoría, con cantidad y dónde están (nevera, despensa…)
- Tres estados por artículo: hay, queda poco, no hay
- Apuntar un artículo en la lista de la compra con un toque, o todo lo que falta de una vez
- Filtro de "por reponer" y búsqueda

## Cómo se habla con la app de la compra

- Al finalizar una compra, lo que estaba marcado entra solo en el inventario. Lo hace un trigger en la base de datos, así que la app de la compra no necesita saber que el inventario existe.
- La app de la compra lee el inventario para avisar ("ya hay 2 en casa") al apuntar un producto y en el modo compras.
- Desde aquí se escribe en la lista de la compra cuando algo se acaba.

Los productos se cruzan por nombre. Es el punto débil: "Leche" y "Leche entera" son dos cosas distintas.

## Decisiones

- **Sin base de datos local.** El inventario se toca en casa, con wifi. Ir directo a la API ahorra toda la sincronización, que en la app de la compra fue lo más delicado. A cambio, sin conexión no funciona.
- **Sondeo cada 5 segundos en vez de websocket.** Suficiente para una pantalla que usa una persona, y no hay conexión que vigilar.
- **Estados antes que cantidades exactas.** Nadie descuenta cada yogur. Marcar "queda poco" sí se hace.

## Para compilar

Hacen falta dos claves en `local.properties` (no se sube al repositorio):

```
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
```

La tabla `inventory` y el trigger están en el `schema.sql` del repositorio de la web.

## Pendiente

- Código de barras, para dejar de depender del nombre
- Caducidades
- Login: ahora mismo la base de datos está abierta con la clave anónima
