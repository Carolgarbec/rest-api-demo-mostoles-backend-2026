package com.example.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.example.dto.ProductDto;
import com.example.entities.Presentation;
import com.example.entities.Product;
import com.example.services.ProductService;
import com.example.spring_security_jwt.payload.request.LogginRequest;
import com.example.utilities.FileDownloadUtil;
import com.example.utilities.FileUploadUtil;
import com.example.utilities.FileUtil;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

// @WebMvcTest(ProductController.class)
@SpringBootTest

/**
 * La anotacion anterior es la recomendada para implementar test de Integracion,
 * a la capa de controladores que conlleva la realizacion de peticiones HTTP.
 * 
 * Esta anotacion no serviria si tuviesemos implementada la seguridad con Spring
 * Security porque no carga todo el contexto de Spring. Cuando se implemente la
 * seguridad, comentaremos esta anotacion y utilizaremos @SpringBootTest
 */

/*
 * La siguiente anotacion se utiliza cuando queremos utilizar una base de datos
 * real, que no sea H2 Database, MySQL por ejemplo, y que al terminar la prueba
 * se deje la base de datos tal y como estaba
 */
@AutoConfigureTestDatabase(replace = Replace.NONE)

/**
 * Se necesita MockMvc para realizar peticiones a los end points, lo cual
 * suministra y configura la anotacion siguiente
 */
@AutoConfigureMockMvc
class ProductControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ProductService productService;

	@MockitoBean
	FileUploadUtil fileUploadUtil;

	@MockitoBean
	FileDownloadUtil fileDownloadUtil;

	@MockitoBean
	FileUtil fileUtil;

	@Autowired
	ObjectMapper objectMapper;

	List<Product> products = new ArrayList<>();
	Presentation presentation1, presentation2;
	Product product1, product2;
	ProductDto productDto1, productDto2;

	String token;
	String userToken;

	@BeforeEach
	void setUp() throws Exception {

		/**
		 * Necesitamos obtener un token valido para presentarlo en cada test. Se
		 * obtienen dos: uno de un usuario ADMIN (admin1) y otro de un usuario con rol
		 * USER (user1), para poder comprobar los enlaces HATEOAS que se anuncian a cada
		 * uno.
		 */

		this.token = obtenerToken("admin1", "Temp2026$$##");

		this.userToken = obtenerToken("user1", "Temp2026$$##");

		// ------------------------------------------------------------------------

		presentation1 = Presentation.builder().name("decenas").description("Por decenas").build();
		presentation1.setId(1);

		presentation2 = Presentation.builder().name("unidades").description("Por unidades").build();
		presentation2.setId(2);

		product1 = Product.builder().name("Camara").description("HP Camara").price(new BigDecimal(500)).stock(1900)
				.productImage(null).presentation(presentation1).build();
		product1.setId(1);

		product2 = Product.builder().name("Frigorifico").description("General Electric").price(new BigDecimal(2500))
				.stock(3900).productImage(null).presentation(presentation2).build();
		product2.setId(2);

		products.add(product1);
		products.add(product2);

		/**
		 * Los ProductDto (capa de presentacion, el Record ProductDto) son los que
		 * viajan en el cuerpo de las peticiones POST/PUT (parte multipart "product"), y
		 * referencian la presentacion por su id (presentationId)
		 */
		productDto1 = new ProductDto(product1.getName(), product1.getDescription(), product1.getStock(),
				product1.getPrice(), presentation1.getId(), product1.getProductImage());

		productDto2 = new ProductDto(product2.getName(), product2.getDescription(), product2.getStock(),
				product2.getPrice(), presentation2.getId(), product2.getProductImage());
	}

	/**
	 * Realiza el login contra /api/auth/signin y devuelve el token JWT listo para
	 * usarse en la cabecera Authorization.
	 */
	private String obtenerToken(String username, String password) throws Exception {

		LogginRequest logginRequest = LogginRequest.builder().username(username).password(password).build();

		String jsonLogginRequest = objectMapper.writeValueAsString(logginRequest);

		ResultActions resultActions = this.mockMvc
				.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON).content(jsonLogginRequest));

		MvcResult mvcResult = resultActions.andReturn();

		String contentAsString = mvcResult.getResponse().getContentAsString();

		JSONObject jsonObject = new JSONObject(contentAsString);

		return "Bearer " + jsonObject.getString("token");
	}

	@Test
	@DisplayName("Controller Test que recupera todos los productos")
	void testFindAll() throws Exception {

		// given

		given(productService.findAll(Sort.by("name"))).willReturn(products);

		// when => Realizar la peticion (request) HTTP, mediante el metodo GET
		// al end point de products ("/products"). Aqui se utiliza MockMvc

		ResultActions response = mockMvc
				.perform(get("/products").accept(MediaType.APPLICATION_JSON).header("Authorization", this.token));
		// then

		response.andExpect(status().isOk()).andDo(print())
				.andExpect(jsonPath("$._embedded.productList.size()", is(products.size())))
				.andExpect(jsonPath("$._embedded.productList[0].name", is(product1.getName())))
				.andExpect(jsonPath("$._embedded.productList[0].presentation.id", is(presentation1.getId())))
				.andExpect(jsonPath("$._embedded.productList[0]._links.update.href", containsString("/products/1")))
				.andExpect(jsonPath("$._embedded.productList[0]._links.delete.href", containsString("/products/1")))
				.andExpect(jsonPath("$._links.self.href", containsString("/products")))
				.andExpect(jsonPath("$._links.create.href", containsString("/products")));

	}

	@Test
	@DisplayName("Controller Test que recupera los productos paginados con PagedModel")
	void testFindAllPaginado() throws Exception {

		// given
		int page = 0;
		int size = 2;
		Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
		Page<Product> productPage = new PageImpl<>(products, pageable, 5);

		given(productService.findAll(pageable)).willReturn(productPage);

		// when / then
		mockMvc.perform(get("/products").param("page", String.valueOf(page)).param("size", String.valueOf(size))
				.accept(MediaType.APPLICATION_JSON).header("Authorization", this.token)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("$._embedded.productList.size()", is(products.size())))
				.andExpect(jsonPath("$.page.size", is(size))).andExpect(jsonPath("$.page.number", is(page)))
				.andExpect(jsonPath("$.page.totalElements", is(5))).andExpect(jsonPath("$.page.totalPages", is(3)))
				.andExpect(jsonPath("$._links.self.href", containsString("/products")))
				.andExpect(jsonPath("$._links.create.href", containsString("/products")))
				.andExpect(jsonPath("$._links.first.href", containsString("page=0")))
				.andExpect(jsonPath("$._links.next.href", containsString("page=1")))
				.andExpect(jsonPath("$._links.last.href", containsString("page=2")));

	}

	@Test
	@DisplayName("Controller Test que NO anuncia enlaces de mutacion de un producto a un usuario USER")
	void testProductoSinEnlacesDeMutacionParaUsuarioNoAdmin() throws Exception {

		// given
		given(productService.findById(1)).willReturn(product1);

		// when / then: un usuario con rol USER no debe ver los enlaces update ni delete
		mockMvc.perform(
				get("/products/{id}", 1).accept(MediaType.APPLICATION_JSON).header("Authorization", this.userToken))
				.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.name", is(product1.getName())))
				.andExpect(jsonPath("$._links.self.href", containsString("/products/1")))
				.andExpect(jsonPath("$._links['all-products'].href", containsString("/products")))
				.andExpect(jsonPath("$._links.update").doesNotExist())
				.andExpect(jsonPath("$._links.delete").doesNotExist());
	}

	@Test
	@DisplayName("Controller Test que NO anuncia el enlace create de la coleccion a un usuario USER")
	void testColeccionSinEnlaceCreateParaUsuarioNoAdmin() throws Exception {

		// given
		given(productService.findAll(Sort.by("name"))).willReturn(products);

		// when / then: un usuario con rol USER no debe ver el enlace create ni los de
		// mutacion de cada producto
		mockMvc.perform(get("/products").accept(MediaType.APPLICATION_JSON).header("Authorization", this.userToken))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.productList[0]._links.self.href", containsString("/products/1")))
				.andExpect(jsonPath("$._embedded.productList[0]._links.update").doesNotExist())
				.andExpect(jsonPath("$._embedded.productList[0]._links.delete").doesNotExist())
				.andExpect(jsonPath("$._links.create").doesNotExist());
	}

	@Test
	@DisplayName("Controller Test para Persistir un Producto")
	void testSaveProduct() {

		// given
		given(productService.save(any(Product.class))).willAnswer(invocation -> {
			Product productoAGuardar = invocation.getArgument(0);
			productoAGuardar.setId(1);
			return productoAGuardar;
		});

		// when

		/*
		 * Convertir el ProductDto a formato JSON, es decir, una cadena (String) en
		 * formato de JSON, lo cual hace el objectMapper que hemos inyectado como
		 * dependencia al principio de la clase bajo Test
		 */

		String jsonStringProduct = objectMapper.writeValueAsString(productDto1);

		MockMultipartFile bytesArrayProduct = new MockMultipartFile("product", null, "application/json",
				jsonStringProduct.getBytes());

		try {
			mockMvc.perform(multipart("/products").file(bytesArrayProduct).file("file", null).header("Authorization",
					this.token)).andDo(print()).andExpect(status().isCreated())
					.andExpect(jsonPath("$.name", is(product1.getName())))
					.andExpect(jsonPath("$.presentation.id", is(presentation1.getId())))
					.andExpect(jsonPath("$._links.self.href", containsString("/products/1")));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// then

	}

	@Test
	@DisplayName("Controller Test para recuperar un producto por su ID")
	void testRecuperarProductoPorSuID() throws Exception {

		// given

		int productId = 1;

		given(productService.findById(productId)).willReturn(product1);

		// when
		mockMvc.perform(get("/products/{id}", productId).header("Authorization", this.token)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("$.name", is(product1.getName())))
				.andExpect(jsonPath("$.presentation.id", is(presentation1.getId())))
				.andExpect(jsonPath("$._links.self.href", containsString("/products/1")))
				.andExpect(jsonPath("$._links['all-products'].href", containsString("/products")));
	}

	@Test
	@DisplayName("Controller Test Producto no encontrado")
	void testProductoNoEncontrado() throws Exception {

		// given
		given(productService.findById(20)).willReturn(null);

		// when

		mockMvc.perform(get("/products/{id}", 20).header("Authorization", this.token)).andDo(print())
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Controller Test que actualiza un producto con su imagen")
	void testUpdateProduct() throws Exception {

		// given
		int id = 1;
		given(productService.findById(id)).willReturn(product1);
		given(productService.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		String jsonStringProduct = objectMapper.writeValueAsString(productDto1);

		MockMultipartFile bytesArrayProduct = new MockMultipartFile("product", null, "application/json",
				jsonStringProduct.getBytes());

		ResultActions response = this.mockMvc.perform(multipart("/products/{id}", id).with(request -> {
			request.setMethod("PUT");
			return request;
		}).file("image", null).file(bytesArrayProduct).header("Authorization", this.token));

		// then
		response.andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.name", is(product1.getName())))
				.andExpect(jsonPath("$.description", is(product1.getDescription())))
				.andExpect(jsonPath("$.presentation.id", is(presentation1.getId())))
				.andExpect(jsonPath("$._links.self.href", containsString("/products/1")));

	}

	@Test
	@DisplayName("Controller Test que elimina un producto")
	void testDeleteProduct() throws Exception {

		// given
		int ProductId = 1;

		given(productService.findById(ProductId)).willReturn(product1);
		doNothing().when(productService).delete(product1);

		// when
		mockMvc.perform(delete("/products/{id}", ProductId).header("Authorization", this.token))
				.andExpect(status().isOk());

	}

	/****************** ESTO SON LOS CAMBIOS QUE PIDE EL EXAMEN DE LA PARTE DE TEST PUNTOS 3,4 Y 5 *********************/

	/**
	 * Test de integracion del end point GET /products/products-by-presentation con
	 * un usuario NO registrado (sin token JWT), y por tanto NO autenticado.
	 */

	/**
	 * Verifica que un usuario no autenticado no puede consultar los productos por
	 * presentación. Se lanza un GET a
	 * {@code /products/products-by-presentation?presentation=...} sin la cabecera
	 * {@code Authorization} y se comprueba que: responde 401 Unauthorized; la
	 * petición es rechazada por la capa de seguridad antes de llegar al controller,
	 * por lo que el servicio nunca se invoca.
	 *
	 *
	 *para ejecutar el test en terminal seria: mvn test -Dtest=ProductControllerTest#testProductsByPresentationUsuarioNoAutenticado
	 */
	@Test
	@DisplayName("Controller Test productos por presentacion con usuario no autenticado")
	void testProductsByPresentationUsuarioNoAutenticado() throws Exception {

		// when => peticion SIN la cabecera Authorization
		ResultActions response = mockMvc.perform(get("/products/products-by-presentation")
				.param("presentation", "unidad").accept(MediaType.APPLICATION_JSON));

		// then
		response.andDo(print()).andExpect(status().isUnauthorized());

		verify(productService, never()).findByPresentationName(any());
	}

	/**
	 * Test del end point GET /products/products-by-presentation con un usuario
	 * registrado, es decir, que envia su token JWT.
	 * 
	 * Lo que quiero comprobar es que, si el usuario ha hecho login, el end point le
	 * responde con los productos de la presentacion que ha pedido.
	 * 
	 * Para ejecutar el test desde la terminal con maven:
	 * mvn test -Dtest=ProductControllerTest#testProductsByPresentationUsuarioAutenticado
	 */
	@Test
	@DisplayName("Controller Test productos por presentacion con usuario registrado")
	void testProductsByPresentationUsuarioAutenticado() throws Exception {

		// given => le digo al servicio falso (Mockito) que, cuando le pidan los
		// productos de la presentacion "unidad", devuelva una lista con solo product2
		List<Product> productosDeLaPresentacion = List.of(product2);
		given(productService.findByPresentationName(presentation2.getName()))
				.willReturn(productosDeLaPresentacion);

		// when => hago la peticion GET al end point, mandando el parametro
		// presentation y el token del usuario registrado en la cabecera Authorization
		ResultActions response = mockMvc.perform(get("/products/products-by-presentation")
				.param("presentation", presentation2.getName())
				.header("Authorization", this.userToken));

		// then => compruebo la respuesta, paso a paso

		// 1. El codigo tiene que ser 200 (todo OK)
		response.andExpect(status().isOk());

		// 2. La lista tiene que tener un solo producto
		response.andExpect(jsonPath("$._embedded.productList.size()", is(1)));

		// 3. Ese producto tiene que ser product2, o sea, el nombre tiene que coincidir
		response.andExpect(jsonPath("$._embedded.productList[0].name", is(product2.getName())));

		// 4. Su presentacion tiene que ser la que he pedido
		response.andExpect(jsonPath("$._embedded.productList[0].presentation.name",
				is(presentation2.getName())));
	}

	/*********************************************************************************************************/

}
