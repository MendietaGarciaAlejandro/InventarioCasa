# Inventario de casa

App Android para llevar la cuenta de lo que hay en casa. Nació de un problema concreto: comprar algo que ya teníamos porque nadie se acordaba.

Comparte base de datos (Supabase) con la app de la lista de la compra y con su web, pero es una app aparte a propósito: la de la compra la usa mi padre y quería que siguiera igual de simple.

## Qué hace

- Lista de artículos por categoría, con cantidad y dónde están (nevera, despensa…)
- Tres estados por artículo: hay, queda poco, no hay
- Fecha de caducidad opcional, con aviso cuando falta una semana o menos
- Apuntar un artículo en la lista de la compra con un toque, o todo lo que falta de una vez
- Filtro de "por reponer" y búsqueda
- Escanear el código de barras de un producto para encontrarlo o darlo de alta

## Cómo se habla con la app de la compra

- Al finalizar una compra, lo que estaba marcado entra solo en el inventario. Lo hace un trigger en la base de datos al borrarse un producto marcado. Quitar un producto de la lista sin haberlo comprado no cuenta: en ese caso se desmarca antes de borrarlo.
- La app de la compra lee el inventario para avisar ("ya hay 2 en casa") al apuntar un producto y en el modo compras.
- Desde aquí se escribe en la lista de la compra cuando algo se acaba.

Los productos se cruzan por nombre. Es el punto débil: "Leche" y "Leche entera" son dos cosas distintas.

## Códigos de barras

Un código de barras es solo un número; no dice qué producto es. La app lo resuelve en dos pasos:

1. Mira si ese código ya está asociado a un artículo del inventario (tabla `inventory_barcodes`). Si lo está, abre ese artículo.
2. Si es la primera vez, pregunta a [Open Food Facts](https://world.openfoodfacts.org), una base de datos abierta, y propone el nombre que tenga. A partir de ahí se crea un artículo nuevo o se asocia el código a uno que ya existía.

Un artículo puede tener varios códigos (otra marca, otro tamaño), porque en casa "Leche" es "Leche" sea de la marca que sea. Lo que no cubre Open Food Facts (droguería, productos poco comunes) se nombra a mano la primera vez.

El escáner es el de Google Play Services: la app no pide permiso de cámara, pero necesita un móvil con Play Services.

## Decisiones

- **Sin base de datos local.** El inventario se toca en casa, con wifi. Ir directo a la API ahorra toda la sincronización, que en la app de la compra fue lo más delicado. A cambio, sin conexión no funciona.
- **Sondeo cada 5 segundos en vez de websocket.** Suficiente para una pantalla que usa una persona, y no hay conexión que vigilar.
- **Estados antes que cantidades exactas.** Nadie descuenta cada yogur. Marcar "queda poco" sí se hace.
- **Una sola fecha de caducidad por artículo**, la de la unidad que antes caduca. Guardar una por unidad sería más exacto y nadie lo mantendría. Al acabarse el artículo la fecha se borra.

## Para compilar

Hacen falta dos claves en `local.properties` (no se sube al repositorio):

```
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
```

La tabla `inventory` y el trigger están en el `schema.sql` del repositorio de la web.

## Pendiente

- Usar el código de barras también al comprar, para que la lista y el inventario dejen de cruzarse solo por nombre
