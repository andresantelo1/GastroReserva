package com.example.gastroreservabackend1;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MigrationUpgradeIntegrationTest {
    @Autowired DataSource dataSource;

    @Test
    void actualizaV8AV9ConservandoReservaEHistorialPrevio() {
        String schema = "gastro_v9_" + UUID.randomUUID().toString().replace("-", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create schema " + schema);
        String p = schema + ".";
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("8").load().migrate();
            jdbc.execute("insert into " + p + "usuarios(id,nombre,email,password_hash,rol) values (1,'Host','migration@example.test','fixture','HOST')");
            jdbc.execute("insert into " + p + "clientes(id,nombre,email) values (1,'Cliente previo','previo@example.test')");
            jdbc.execute("insert into " + p + "zonas(id,nombre,activa) values (1,'Zona previa',true)");
            jdbc.execute("insert into " + p + "mesas(id,numero,capacidad,estado,activa,zona_id) values (1,1,4,'DISPONIBLE',true,1)");
            jdbc.execute("insert into " + p + "turnos(id,nombre,hora_inicio,hora_fin,capacidad_maxima) values (1,'Turno previo','12:00:00','15:00:00',20)");
            jdbc.execute("insert into " + p + "reservas(id,cliente_id,turno_id,mesa_id,fecha,inicio,fin,cantidad_personas,estado,creado_por_usuario_id) values (1,1,1,1,'2030-01-15','2030-01-15 12:00:00','2030-01-15 15:00:00',2,'SOLICITADA',1)");
            jdbc.execute("insert into " + p + "historial_reservas(id,reserva_id,estado_nuevo,motivo,cambiado_por_usuario_id,tipo_evento,mesa_nueva_id) values (1,1,'SOLICITADA','Reserva previa',1,'CREACION',1)");
            var previo = jdbc.queryForMap("select id,reserva_id,estado_nuevo,motivo,tipo_evento,mesa_nueva_id,creado_en from " + p + "historial_reservas where id=1");
            Flyway actual = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load();
            actual.migrate();
            assertThat(actual.validateWithResult().validationSuccessful).isTrue();
            assertThat(actual.info().current().getVersion().getVersion()).isEqualTo("9");
            assertThat(jdbc.queryForMap("select id,reserva_id,estado_nuevo,motivo,tipo_evento,mesa_nueva_id,creado_en from " + p + "historial_reservas where id=1")).isEqualTo(previo);
            assertThat(jdbc.queryForObject("select cliente_id from " + p + "reservas where id=1", Long.class)).isEqualTo(1L);
            assertThat(jdbc.queryForObject("select cliente_anterior_id from " + p + "historial_reservas where id=1", Long.class)).isNull();
        } finally {
            jdbc.execute("drop schema " + schema + " cascade");
        }
    }

    @Test
    void actualizaV5HastaV9SinPerderDatosExistentes() {
        // Sólo esquema nuevo, aleatorio y exclusivo dentro de la base de pruebas.
        String schema = "gastro_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create schema " + schema);
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("5").load().migrate();
            jdbc.update("insert into " + schema + ".zonas(nombre, descripcion, activa) values (?, ?, ?)",
                    "Zona previa", "Debe conservarse", true);
            Flyway actual = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load();
            actual.migrate();
            assertThat(actual.info().current().getVersion().getVersion()).isEqualTo("9");
            assertThat(actual.info().pending()).isEmpty();
            assertThat(actual.validateWithResult().validationSuccessful).isTrue();
            assertThat(jdbc.queryForObject("select descripcion from " + schema + ".zonas where nombre = 'Zona previa'", String.class))
                    .isEqualTo("Debe conservarse");
            assertThat(jdbc.queryForObject("select count(*) from " + schema + ".pedidos", Long.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from " + schema + ".feedback", Long.class)).isZero();
        } finally {
            jdbc.execute("drop schema " + schema + " cascade");
        }
    }
}
