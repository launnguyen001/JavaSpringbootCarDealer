package com.example.lab5.repository;

import com.example.lab5.entity.Brand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Repository
public class JdbcBrandRepository implements BrandRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Brand> brandRowMapper = (rs, rowNum) -> {
        Brand brand = new Brand();
        brand.setId(rs.getLong("id"));
        brand.setName(rs.getString("name"));
        brand.setCountry(rs.getString("country"));
        brand.setDescription(rs.getString("description"));
        return brand;
    };

    public JdbcBrandRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Brand save(Brand brand) {
        String sql = "INSERT INTO brands (name, country, description) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, brand.getName());
            statement.setString(2, brand.getCountry());
            statement.setString(3, brand.getDescription());
            return statement;
        }, keyHolder);

        brand.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return brand;
    }

    @Override
    public List<Brand> findAll() {
        String sql = "SELECT id, name, country, description FROM brands ORDER BY id";
        return jdbcTemplate.query(sql, brandRowMapper);
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM brands WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
