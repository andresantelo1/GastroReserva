package com.example.gastroreservabackend1.repository;

import com.example.gastroreservabackend1.model.ProductoMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductoMenuRepository extends JpaRepository<ProductoMenu, Long>,
        JpaSpecificationExecutor<ProductoMenu> {

    boolean existsByNombreNormalizado(String nombreNormalizado);

    boolean existsByNombreNormalizadoAndIdNot(String nombreNormalizado, Long id);
}
