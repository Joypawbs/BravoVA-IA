# Traducciones de BravoVa

`strings-en.xml` es el inglés (para la diáspora). Está aquí, fuera de `composeResources`,
para que la app no mezcle idiomas mientras se traduce: cuando todos los textos estén en
`composeResources/values/strings.xml` y traducidos, se mueve a
`composeApp/src/commonMain/composeResources/values-en/strings.xml` (fase 4 del plan 2.0).
