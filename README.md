# Inventario de casa

Una app Android para saber qué hay en casa. La hice porque comprábamos cosas que ya teníamos: nadie se acordaba de si quedaba aceite y, por si acaso, se compraba otra botella.

Va aparte de la app de la lista de la compra a propósito. Esa la usa mi padre y no quería llenarla de pantallas nuevas, así que las dos comparten la base de datos (Supabase), cada una con su interfaz.

## Qué hace

Cada artículo tiene categoría, cantidad, dónde está (nevera, despensa…) y un estado: hay, poco o no hay. El estado pesa más que la cantidad, porque en casa nadie descuenta cada yogur, pero ver que queda poco sí se hace. Bajar la cantidad a cero lo deja en "no hay", y reponerlo lo devuelve a "hay".

Se puede poner una fecha de caducidad; cuando falta una semana o menos, el artículo se marca. Es una sola fecha por artículo, la de lo que antes caduque. Una por unidad sería más exacto, pero nadie la mantendría. Al acabarse el artículo, la fecha se borra.

Lo que falta se apunta en la lista de la compra con un toque, o todo de golpe desde el filtro "por reponer". Borrar un artículo pide confirmación, porque es fácil darle sin querer.

## Con la app de la compra

Las dos apps se pasan información en ambos sentidos:

- Al finalizar una compra, lo comprado entra solo en el inventario. Lo hace un trigger en la base de datos cuando se borra un producto marcado de la lista. Si un producto se quita de la lista sin comprarlo, la otra app lo desmarca antes de borrarlo y no suma.
- La app de la compra lee el inventario y avisa ("ya hay 2 en casa") al apuntar algo.

Se cruzan por nombre, y es lo más débil de todo: "Leche" y "Leche entera" son dos productos distintos. Para que al menos "leche" y "Leche" sean el mismo, los tres clientes guardan el nombre igual, con la primera letra en mayúscula.

## Códigos de barras

El código de barras de un producto es solo un número; por sí mismo no dice qué es. Al escanear, la app mira primero si ese código ya está asociado a algún artículo de casa y, si lo está, lo abre. Si no, pregunta a [Open Food Facts](https://world.openfoodfacts.org) y propone el nombre que tengan. Con ese nombre se crea el artículo, o el código se asocia a uno que ya existía.

Un artículo puede tener varios códigos: la leche es "Leche" sea de la marca que sea. Lo que no está en Open Food Facts (droguería, cosas del estanco) se nombra a mano la primera vez y ya queda.

Un detalle que me costó encontrar: algunos productos llevan un UPC-A de 12 cifras, que es el mismo código que un EAN-13 con un cero delante. El escáner del móvil y el de la web no lo devolvían igual, así que el mismo producto acababa con dos códigos. Ahora se guardan siempre con 13 cifras.

El escáner es el de Google Play Services. No hace falta pedir permiso de cámara, pero solo funciona en móviles con Play Services.

## Por qué no hay base de datos local

La app de la compra tiene Room y sincroniza con el servidor, y esa sincronización fue lo que más guerra dio de todo el proyecto. El inventario se usa en casa, con wifi, así que aquí va directo contra la API y pregunta cada 5 segundos por si algo ha cambiado desde la web o desde una compra. Sin conexión no funciona, y lo acepto.

## Compilar

Dos claves en `local.properties`, que no se sube al repositorio:

```
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
```

Las tablas y el trigger están en `supabase/schema.sql`, en el repositorio de la web. Los tests de `ArticuloTest` cubren las reglas de cantidad, estado y caducidad, que es donde más fácil era equivocarse.

## Pendiente

- Usar el código de barras también al comprar, para que la lista y el inventario dejen de depender del nombre
