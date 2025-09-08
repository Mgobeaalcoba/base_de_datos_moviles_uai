---

### **Documento de Implementación: MVP de App de Notas (TP NRO 02\)**

#### **Gobea Alcoba, Mariano Daniel**

#### **1\. Resumen Ejecutivo**

El presente documento describe la estrategia de implementación de un MVP para una aplicación móvil de notas, "AppTrack Notes". El objetivo es demostrar la correcta gestión de datos persistentes utilizando **Room (SQLite)** para la persistencia local y **Firebase Firestore** como servicio de sincronización en la nube, cumpliendo con los requisitos de las Partes 1, 2 y 3 del TP Nro 02\. La solución se centra en un enfoque "offline-first", garantizando la usabilidad incluso sin conexión a internet.

#### **2\. Arquitectura de la Solución**

La arquitectura de la aplicación sigue el patrón **MVVM (Model-View-ViewModel)**. La capa de datos (Model) se divide en dos fuentes principales: una base de datos local gestionada por Room y una fuente remota proporcionada por Firebase Firestore. El **ViewModel** actuará como intermediario, manejando la lógica de negocio y la sincronización.

**Componentes Principales:**

* **Activity/Fragment (View):** La interfaz de usuario que muestra las notas y permite la interacción del usuario. Observa los datos del ViewModel.  
* **ViewModel:** Contiene la lógica para gestionar los datos. Expone un LiveData o StateFlow con la lista de notas. Interactúa con el Repositorio.  
* **Repositorio:** Abstrae la fuente de datos. Decide si los datos se obtienen de la base de datos local o de la nube.  
* **Base de Datos Local (Room):** Persiste las notas localmente.  
* **Servicio de Nube (Firebase Firestore):** Persiste las notas remotamente y facilita la sincronización.

#### **3\. Parte 1: Persistencia Local (Room)**

Para la persistencia local, se utilizará la biblioteca **Room**, que es la capa de abstracción recomendada por Google sobre SQLite. Room ofrece un ORM (Object-Relational Mapping) que facilita la creación y gestión de la base de datos sin necesidad de escribir código SQL de forma manual.

Modelo de Datos:  
Se creará una entidad de datos simple para representar una nota. La clave principal será un UUID para garantizar la unicidad tanto localmente como en la nube.

* **Entidad Note (Kotlin Data Class):**  
  * id: String (UUID) \- Clave primaria.  
  * title: String  
  * content: String  
  * timestamp: Long (para ordenamiento y gestión de conflictos).  
  * isSynced: Boolean (opcional, para indicar si se ha subido a la nube).

Implementación de CRUD:  
Se creará un DAO (Data Access Object) para definir los métodos de interacción con la base de datos.

* @Insert: Insertar una nueva nota.  
* @Update: Actualizar una nota existente.  
* @Delete: Borrar una nota.  
* @Query("SELECT \* FROM notes ORDER BY timestamp DESC"): Consultar todas las notas.

#### **4\. Parte 2: Sincronización en la Nube (Firebase Firestore)**

Se utilizará Firebase Firestore por su capacidad de operar **offline-first** de forma nativa. Esto significa que los cambios se escriben primero en una caché local persistente y luego se sincronizan automáticamente con el servidor cuando la conexión está disponible.

Configuración de Firebase:  
Para conectar la aplicación con el proyecto de Firebase, se debe incluir el archivo  
google-services.json 1 en el directorio raíz del módulo de la aplicación (

/app). Este archivo contiene la configuración necesaria para la conexión, incluyendo el

project\_id ("apptrack-solutions") 2y el

storage\_bucket ("apptrack-solutions.firebasestorage.app")3.

Además, se deben agregar las dependencias y el plugin de Google Services en los archivos build.gradle del proyecto y del módulo para que el archivo JSON sea procesado y se genere la configuración necesaria en tiempo de compilación.

**Estrategia de Sincronización:**

* **Offline-First por defecto:** Gracias a Firebase Firestore, la app funcionará sin necesidad de una lógica de conexión manual. Las operaciones CRUD realizadas localmente se pondrán en cola y se enviarán a Firestore automáticamente al restablecer la conexión.  
* **Resolución de Conflictos (Estrategia "Última escritura gana"):** Para simplificar el MVP y cumplir con los requisitos, se implementará una estrategia de resolución de conflictos básica. Cada nota tendrá un campo timestamp que se actualizará con cada modificación. Cuando se produzca un conflicto, el documento con el timestamp más reciente sobrescribirá al anterior. Esto se gestiona automáticamente por Firestore si se usa set() con merge: true o simplemente update().

**Flujo de Sincronización Sencillo:**

1. **Creación de Nota:** El usuario crea una nota.  
   * Se genera un UUID.  
   * Se inserta en la base de datos local (Room).  
   * Se envía a Firestore.  
2. **Actualización de Nota:** El usuario edita una nota.  
   * Se actualiza en la base de datos local (Room).  
   * Se envía a Firestore con el nuevo timestamp.  
3. **Borrado de Nota:** El usuario borra una nota.  
   * Se borra de la base de datos local (Room).  
   * Se borra de Firestore.

#### **5\. Parte 3: Seguridad y Privacidad (Autenticación)**

Para el MVP, se implementará la **autenticación de usuario** como técnica de seguridad para el acceso a los datos. Firebase Authentication es la solución más sencilla y robusta para este propósito.

**Implementación de Seguridad:**

1. **Firebase Authentication:** Se configurará la autenticación anónima para el MVP. Esto nos permite tener un userId único por cada usuario sin requerir un registro completo (email/contraseña), cumpliendo el requisito de autenticación de forma simple.  
2. **Reglas de Seguridad de Firestore:** Se establecerán reglas en Firestore para que solo el usuario autenticado (a través de su userId) pueda leer y escribir en su colección de notas. Esto asegura que los datos de un usuario no sean accesibles para otros.  
   * match /notes/{userId}/{noteId}  
   * allow read, write: if request.auth.uid \== userId;

**Impacto en la Usabilidad y Performance:**

* **Usabilidad:** La autenticación anónima es transparente para el usuario final, no requiere pasos adicionales de registro y mejora la usabilidad.  
* **Performance:** El uso de Firebase Authentication y las reglas de seguridad de Firestore no tienen un impacto significativo en la performance, ya que la validación de permisos es muy rápida y está optimizada por Firebase.

#### **6\. Conclusión y Próximos Pasos**

El MVP propuesto cumple con los requisitos del TP utilizando un enfoque pragmático y a prueba de fallas. La combinación de Room y Firebase Firestore, junto con la autenticación anónima, proporciona una solución robusta y escalable que puede ser expandida en futuras iteraciones.

Con esta base, podríamos explorar las siguientes mejoras:

* Implementar una estrategia de resolución de conflictos más avanzada, como la lógica "primero en llegar, primero en ser servido" o una resolución manual por el usuario.  
* Añadir otras técnicas de seguridad, como el cifrado de la base de datos local.  
* Incorporar una funcionalidad de IA, como la categorización automática de notas.