package com.example.controllers;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.assemblers.ProductModelAssembler;
import com.example.dto.ProductDto;
import com.example.entities.Product;
import com.example.mappers.ProductMapper;
import com.example.services.ProductService;
import com.example.utilities.FileDownloadUtil;
import com.example.utilities.FileUploadUtil;
import com.example.utilities.FileUtil;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * La anotacion @RestController es para que todos los metodos que van a ser
 * creados dentro de este controlador y reciben peticiones a través del
 * protocolo HTTP, mediante los verbos correspondientes (GET, POST, PUT, DELETE,
 * PATCH, etc.) devuelvan o reciban datos en formato de JSON (JavaScript Object
 * Notation)
 */

@RestController

/**
 * Una API REST esta orientada al recurso, es decir, que el controlador necesita
 * que se le especifique que recurso va a responder, por ejemplo en esto seria
 * /products, y en dependencia del verbo del protocolo HTTP se estaria haciendo
 * una peticion (request) contreta. Por ejemplo: Si el verbo es GET, significa
 * que estamos solicitando todos los productos al recurso /products. Si el verbo
 * es POST significa que queremos recibir un producto en formato JSON, en el
 * cuerpo de la peticion (request) y persistirlo (guardarlo) en las tablas
 * correspondientes
 */
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;
	private final FileUploadUtil fileUploadUtil;
	private final FileDownloadUtil fileDownloadUtil;
	private final FileUtil fileUtil;
	private final ProductMapper productMapper;
	private final ProductModelAssembler productModelAssembler;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
	public ResponseEntity<CollectionModel<Product>> dameProductos(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "size", required = false) Integer size) {

		List<Product> products;
		Sort sort = Sort.by("name");
		CollectionModel<Product> collectionModel;

		// Comprobar si en la peticion (request) me han suministrado los parametros page
		// y size
		if (page != null && size != null) {

			Pageable pageable = PageRequest.of(page, size, sort);

			// Implica devolver los productos paginados, es decir, una pagina de Product
			Page<Product> productPage = productService.findAll(pageable);

			// El ensamblador construye un PagedModel con los metadatos de paginacion y
			// los enlaces de cada producto y de navegacion (self, create, first, prev y
			// next)
			collectionModel = productModelAssembler.toPagedModel(productPage);

		} else {

			// Devolver los productos ordenados, por nombre (name), por ejemplo
			products = productService.findAll(sort);

			// El ensamblador construye los enlaces de cada producto y el self de la
			// coleccion
			collectionModel = productModelAssembler.toCollectionModel(products);
		}

		return new ResponseEntity<>(collectionModel, HttpStatus.OK);
	}

	/**
	 * El metodo siguiente va a responder a una peticion (request) del tipo:
	 * 
	 * http://localhost:8080/products/products-by-presentation?presentation=unidad
	 * 
	 * Devuelve todos los productos cuya presentacion tiene el nombre recibido como
	 * parametro de la peticion. Si no hay ninguno, devuelve una coleccion vacia con
	 * el codigo 200 (OK). Es un end point protegido: requiere un usuario
	 * autenticado (JWT) con rol USER o ADMIN; sin autenticar responde 401.
	 */

	/**************************** ESTO ES LO QUE PIDE EL PUNTO 2 DEL EXAMEN ******************************************/

	/**
	 * En este metodo he implementado el end point listar todos los productos de una
	 * presentacion concreta. La peticion tiene este formato:

	 * 
	 * La parte "?presentation=unidad" es un parametro, que es un dato que le mando
	 * a la direccion. Para recogerlo en el metodo uso @RequestParam. Lo he puesto
	 * como obligatorio (required = true) porque si no me dicen que presentacion
	 * quieren, no puedo buscar nada.
	 * 
	 * Lo que hace el metodo, paso a paso: 
	 * 1. Recibe el nombre de la presentacion.
	 * 2. Se lo pasa al servicio (productService), que se encarga de buscar en la
	 * base de datos los productos que tienen esa presentacion. 
	 * 3. Devuelve la lista de productos con el codigo 200, que significa que todo ha ido bien.
	 * 
	 * Si no hay ningun producto con esa presentacion, tambien devuelvo un 200 pero
	 * con la lista vacia porque  simplemente no hay resultados.
	 * 
	 * Con @PreAuthorize he puesto que solo pueden usarlo los usuarios que han hecho
	 * login (con rol USER o ADMIN), igual que en los otros metodos de consulta. Si
	 * alguien lo intenta sin hacer login, no le deja pasar y recibe un error 401.
	 */

	@GetMapping("/products-by-presentation")
	@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
	public ResponseEntity<CollectionModel<Product>> findProductsByPresentation(
			@RequestParam(name = "presentation", required = true) String presentation) {

		// Recuperar los productos de la presentacion que me han pedido

		List<Product> products = productService.findByPresentationName(presentation);

		// El ensamblador agrega los enlaces HATEOAS a cada producto y el self de la
		// coleccion, igual que en el metodo que devuelve todos los productos
		return new ResponseEntity<>(productModelAssembler.toCollectionModel(products), HttpStatus.OK);
	}

	/***********************************************************************************************/

	/**
	 * El metodo siguiente recupera un Producto por el id que se recibe como una
	 * variable en la ruta, mediante un end point (url o uri) que tiene el formato
	 * siguiente:
	 * 
	 * http://localhost:8080/productos/1
	 * 
	 * Donde el valor 1 al final del end point seria el id del producto
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
	public ResponseEntity<RepresentationModel<?>> findProductById(
			@PathVariable(name = "id", required = true) int product_id) {

		Map<String, Object> responseAsMap = new HashMap<>();
		ResponseEntity<RepresentationModel<?>> responseEntity = null;

		try {
			Product product = productService.findById(product_id);
			if (product != null) {
				String successMessage = "El producto con id " + product_id + " ha sido encontrado";
				responseAsMap.put("mensaje todo OK: ", successMessage);

				/**
				 * Capa de presentacion: el ensamblador (ProductModelAssembler) se encarga de
				 * construir todos los enlaces HATEOAS del producto (self, all-products y, si
				 * procede, product-image). El controlador ya no repite la construccion de
				 * enlaces.
				 */
				responseEntity = new ResponseEntity<>(productModelAssembler.toModel(product), HttpStatus.OK);
			} else {
				String failureMessage = "No ha sido encontrado ningun producto con id: " + product_id;
				responseAsMap.put("Error: ", failureMessage);
				responseEntity = new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.NOT_FOUND);
			}
		} catch (DataAccessException e) {
			String errorMessage = "Error grave al buscar el producto con id " + product_id
					+ ", y la causa mas probable es: " + e.getMostSpecificCause().getMessage();
			responseAsMap.put("Error grave: ", errorMessage);
			responseEntity = new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}

	/**
	 * Metodo que recibe por POST el Producto para ser persistido, guardado, y que
	 * valida el JSON recibido, para comprobar si esta bien formado o no
	 * 
	 * Primero: Hay que cambiar lo que recibe el metodo saveProduct, porque ya el
	 * producto no viene ocupando todo el cuerpo de la peticion (request), sino una
	 * parte, y la otra parte la ocupa la imagen del producto
	 * 
	 * Y, muy importante, que no se nos olvide anotar este metodo y todos los que
	 * insertan, crean, eliminan registros en las tablas con la
	 * anotacion @Transactional, y tambien hay que especificar el tipo de archivo
	 * que va a consumir este metodo
	 * 
	 * @throws IOException
	 * 
	 */
	@PostMapping(consumes = "multipart/form-data")
	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<RepresentationModel<?>> saveProduct(
			@Valid @RequestPart(name = "product") ProductDto productDto, BindingResult result,
			@RequestPart(name = "file", required = false) MultipartFile imagenDelProducto) throws IOException {

		List<String> mensajesDeError = new ArrayList<>();
		Map<String, Object> responseAsMap = new HashMap<>();
		ResponseEntity<RepresentationModel<?>> responseEntity = null;

		// Primero, comprobar si hay errores en el producto recibido
		if (result.hasErrors()) {
			// Recuperamos los errores que tiene el producto recibido y se lo informamos al
			// que realizo la peticion (request) de persistir el producto
			List<ObjectError> objectErrors = result.getAllErrors();

			objectErrors.stream().forEach(objectError -> mensajesDeError.add(objectError.getDefaultMessage()));

			responseAsMap.put("El producto tiene los siguientes errores: ", mensajesDeError);
			responseAsMap.put("Producto mal formado: ", productDto);

			responseEntity = new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.BAD_REQUEST);

			return responseEntity;
		}

		/**
		 * Capa de persistencia: convertir el ProductDto recibido (capa de presentacion)
		 * en la entidad Product que entiende JPA/Hibernate, mediante MapStruct
		 */
		Product product = productMapper.toEntity(productDto);

		// Persistimos el producto porque si hemos llegado a este punto es que esta bien
		// formado
		// Pero antes vamos a comprobar si hemos recibido imagen del producto, para
		// guardarla
		// en el sistema de archivo (file system)

		if (imagenDelProducto != null && !imagenDelProducto.isEmpty()) {

			String fileCode = fileUploadUtil.saveFile(imagenDelProducto.getOriginalFilename(), imagenDelProducto);

			product.setProductImage(fileCode + '-' + imagenDelProducto.getOriginalFilename());
		}

		try {
			Product productoPersistido = productService.save(product);

			/**
			 * Capa de presentacion: el ensamblador construye todos los enlaces HATEOAS del
			 * producto (self, all-products y, si tiene imagen, product-image)
			 */
			responseEntity = new ResponseEntity<>(productModelAssembler.toModel(productoPersistido),
					HttpStatus.CREATED);
		} catch (DataAccessException e) {
			responseAsMap.put("Error Grave", "No ha podido ser guardado el producto y la causa mas probable es: "
					+ e.getMostSpecificCause().getMessage());
			responseEntity = new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}

	/**
	 * Metodo que recupera la imagen de un producto, dado el codigo que tiene como
	 * prefijo el nombre de la imagen
	 */
	@GetMapping("/fileDownLoad/{fileCode}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> downloadFile(@PathVariable String fileCode) {

		Resource resource = null;

		try {
			resource = fileDownloadUtil.getFileAsResource(fileCode);
		} catch (IOException ioe) {
			return ResponseEntity.internalServerError().build();
		}

		if (resource == null)
			return new ResponseEntity<>("Imagen del producto no encontrada ", HttpStatus.NOT_FOUND);
		/**
		 * Si estamos en este punto quiere decir que el fichero (imagen del producto) ha
		 * sido encontrado y podemos enviarlo como respuesta a la peticion, como un
		 * fichero adjunto en el cuerpo de la respuesta
		 */

		String contentType = "application/octet-stream";
		String headerValue = "attachment; fileName=\"" + resource.getFilename() + "\"";

		/**
		 * HATEOAS: en este endpoint no tiene sentido envolver el binario de la imagen
		 * en un EntityModel (debe viajar como fichero adjunto en el cuerpo de la
		 * respuesta). No obstante, si se puede informar al cliente, mediante el header
		 * HTTP Link, del enlace hacia la coleccion de productos
		 */
		Link allProductsLink = linkTo(methodOn(ProductController.class).dameProductos(null, null))
				.withRel("all-products");

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
				.header(HttpHeaders.LINK, allProductsLink.toString()).body(resource);
	}

	/**
	 * Metodo que actualiza (update) un producto dado el id del mismo.
	 * 
	 * Es basicamente igual al metodo que persiste el producto. Respondera a una
	 * peticion del tipo siguiente, por ejemplo:
	 * 
	 * http://localhost:8080/productos/3
	 * 
	 * Y no habra ambiguedad con el metodo de buscar un producto por el id, porque
	 * el verbo utilizado del protocolo HTTP sera diferente, PUT en este caso.
	 * 
	 */

	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<RepresentationModel<?>> updateProduct(
			@Valid @RequestPart(name = "product") ProductDto productDto, BindingResult result,
			@RequestPart(name = "file", required = false) MultipartFile imagenDelProducto,
			@PathVariable(name = "id", required = true) int product_id) throws IOException {

		List<String> mensajesDeError = new ArrayList<>();
		Map<String, Object> responseAsMap = new HashMap<>();
		ResponseEntity<RepresentationModel<?>> responseEntity = null;

		// comprobar errores de validación
		if (result.hasErrors()) {

			List<ObjectError> objectErrors = result.getAllErrors();

			objectErrors.stream().forEach(objectError -> {
				mensajesDeError.add(objectError.getDefaultMessage());
			});

			responseAsMap.put("respuesta de error: ", mensajesDeError);
			responseAsMap.put("producto mal formado: ", productDto);
			responseEntity = new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.BAD_REQUEST);

			return responseEntity;
		}
		/**
		 * Persisto (guardo) el producto porque está bien formado compruebo si hay
		 * imagen para guardarla y en tal caso debo eliminar la imagen del producto
		 */
		Product productoParaActualizar = productService.findById(product_id);

		if (productoParaActualizar == null) {

			responseAsMap.put("mensaje de error: ", "producto con id: " + product_id + " no encontrado.");
			return new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.NOT_FOUND);
		}

		/**
		 * Capa de persistencia: convertir el ProductDto recibido (capa de presentacion)
		 * en la entidad Product que entiende JPA/Hibernate, mediante MapStruct
		 */
		Product product = productMapper.toEntity(productDto);

		if (imagenDelProducto != null && !imagenDelProducto.isEmpty()) {

			/**
			 * comprobar si productoParaActualizar tiene imagen y si es así eliminarla
			 */
			if (productoParaActualizar.getProductImage() != null) {
				// Eliminar la imagen asociada
				fileUtil.eliminarArchivo(productoParaActualizar.getProductImage());
			}
			/**
			 * agregar prefijo: código alfanumérico aleatorio con método Apache Commons text
			 * (Lang3) (dependencia Maven -> pom.xml)
			 * 
			 * Beans vs Components: Ahora crearemos un componente en el paquete utilities.
			 * Dentro habrá un método para guardar la imagen en una carpeta y devuelve un
			 * código aleatorio que llevará como prefijo el nombre del fichero original
			 * 
			 * NIO.2 (entrada salida no bloqueante) si no existe la carpeta la creará.
			 */
			String fileCode = fileUploadUtil.saveFile(imagenDelProducto.getOriginalFilename(), imagenDelProducto);

			product.setProductImage(fileCode + '-' + imagenDelProducto.getOriginalFilename());
		}

		try {
			product.setId(product_id);
			Product productoAGuardar = productService.save(product);

			/**
			 * Capa de presentacion: el ensamblador construye todos los enlaces HATEOAS del
			 * producto (self, all-products y, si tiene imagen, product-image)
			 */
			responseEntity = new ResponseEntity<>(productModelAssembler.toModel(productoAGuardar), HttpStatus.OK);

		} catch (DataAccessException e) {

			String errorMessage = "Error grave al actualizado el producto y la causa más probable es "
					+ e.getMostSpecificCause().getMessage();
			// e.getStackTrace();
			responseAsMap.put("Error grave: ", errorMessage);

			responseEntity = new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;

	}

	/**
	 * ==============================================================================================
	 * Metodo para eliminar un producto dado el id
	 */
	@DeleteMapping("/{id}")
	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EntityModel<?>> deleteProducto(@PathVariable Integer id) {

		ResponseEntity<EntityModel<?>> responseEntity = null;
		var responseAsMap = new HashMap<String, Object>();

		try {

			// recuperar el producto y comprobar si hay foto y eliminar el archivo
			Product productToDelete = productService.findById(id);

			if (productToDelete == null) {

				responseAsMap.put("mensaje de error: ", "producto con id: " + id + " no encontrado.");
				return new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.NOT_FOUND);
			}

			if (productToDelete.getProductImage() != null) {
				fileUtil.eliminarArchivo(productToDelete.getProductImage());
			}

			productService.delete(productService.findById(id));
			String successMessage = "El producto con id " + id + ", ha sido eliminado";
			responseAsMap.put("mensaje", successMessage);

			/**
			 * HATEOAS: envolver la respuesta en un EntityModel con un enlace hacia la
			 * coleccion de productos
			 */
			Link allProductsLink = linkTo(methodOn(ProductController.class).dameProductos(null, null))
					.withRel("all-products");
			responseEntity = new ResponseEntity<>(EntityModel.of(responseAsMap, allProductsLink), HttpStatus.OK);
		} catch (DataAccessException e) {
			String errorMessage = "No ha podido ser eliminado el producto cuyo id es: " + id
					+ ", siendo la causa mas probable: " + e.getMostSpecificCause().getMessage();
			responseAsMap.put("mensaje", errorMessage);
			responseEntity = new ResponseEntity<>(EntityModel.of(responseAsMap), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return responseEntity;
	}

}
