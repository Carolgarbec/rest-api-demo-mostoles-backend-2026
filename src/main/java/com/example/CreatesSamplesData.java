package com.example;

import com.example.dao.PresentationDao;
import java.math.BigDecimal;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.entities.Presentation;
import com.example.entities.Product;
import com.example.services.PresentationService;
import com.example.services.ProductService;
import com.example.spring_security_jwt.model.ERole;
import com.example.spring_security_jwt.model.Role;
import com.example.spring_security_jwt.model.User;
import com.example.spring_security_jwt.repository.RoleRepository;
import com.example.spring_security_jwt.repository.UserRepository;

@Configuration
public class CreatesSamplesData {

    private final PresentationDao presentationDao;

	CreatesSamplesData(PresentationDao presentationDao) {
		this.presentationDao = presentationDao;
	}

	@SuppressWarnings("null")
	@Bean
    CommandLineRunner samplesData(ProductService productService,
        PresentationService presentationService,
        RoleRepository roleRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder) {
            
            
        return args -> {

            // Crearemos dos presentaciones, por unidad y por decenas, para los productos
            presentationService.save(Presentation.builder()
                .name("unidad").description("por unidades").build());
            
            presentationService.save(Presentation.builder()
                .name("decenas").description("por decenas").build());

            // Persistiremos varios productos que tengan las presentaciones anteriores

            productService.save(Product.builder()
                .name("rezma de papel")
                .description("Description")
                .price(new BigDecimal(3.75))
                .stock(10)
                .presentation(presentationService.findById(1))
                .build());

            productService.save(Product.builder()
                .name("cartas")
                .description("Description")
                .price(new BigDecimal(1))
                .stock(10)
                .presentation(presentationService.findById(2))
                .build());
                
            productService.save(Product.builder()
                .name("guitarra de juguete")
                .description("Description")
                .price(new BigDecimal(4.5))
                .stock(5)
                .presentation(presentationService.findById(1))
                .build());

            productService.save(Product.builder()
                .name("teclado de computadora")
                .description("Description")
                .price(new BigDecimal(15))
                .stock(5)
                .presentation(presentationService.findById(1))
                .build());

            productService.save(Product.builder()
                .name("teclado para laptop")
                .description("Description")
                .price(new BigDecimal(40))
                .stock(5)
                .presentation(presentationService.findById(1))
                .build());                

            productService.save(Product.builder()
                .name("altavoces bluetooth")
                .description("Description")
                .price(new BigDecimal(15))
                .stock(5)
                .presentation(presentationService.findById(1))
                .build());

            productService.save(Product.builder()
                .name("lapices 2b")
                .description("Description")
                .price(new BigDecimal(1.50))
                .stock(4)
                .presentation(presentationService.findById(2))
                .build());

            productService.save(Product.builder()
                .name("boligrafos")
                .description("de color azul")
                .price(new BigDecimal(2))
                .stock(10)
                .presentation(presentationService.findById(1))
                .build());

            productService.save(Product.builder()
                .name("monitor de 15 pulgadas")
                .description("Description")
                .price(new BigDecimal(40))
                .stock(5)
                .presentation(presentationService.findById(1))
                .build());

            productService.save(Product.builder()
                .name("cargador de movil")
                .description("para telefono samsung")
                .price(new BigDecimal(10))
                .stock(10)
                .presentation(presentationService.findById(1))
                .build());

            productService.save(Product.builder()
                .name("mouse")
                .description("ratón de Apple")
                .price(new BigDecimal(40))
                .stock(100)
                .presentation(presentationService.findById(1))
                .build());
            
            // Agregamos los roles de ADMIN y USER
            Role userRole = roleRepository.save(Role.builder().name(ERole.ROLE_USER).build());
            Role adminRole = roleRepository.save(Role.builder().name(ERole.ROLE_ADMIN).build());
            
            /**
             * Agregamos usuarios con los roles creados, para realizar test de integracion a los 
             * endpoints
             */
            
            userRepository.save(User.builder()
            		.username("admin1")
            		.email("admin1@gmail.com")
            		.roles(Set.of(adminRole))
            		.password(passwordEncoder.encode("Temp2026$$##"))
            		.build());
            
            userRepository.save(User.builder()
            		.username("user1")
            		.email("user1@gmail.com")
            		.roles(Set.of(userRole))
            		.password(passwordEncoder.encode("Temp2026$$##"))
            		.build());
            
        };
    
    }
}
