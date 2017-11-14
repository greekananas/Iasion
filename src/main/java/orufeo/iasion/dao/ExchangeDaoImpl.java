package orufeo.iasion.dao;

import java.io.IOException;
import java.sql.Types;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;


import orufeo.iasion.data.objects.storage.Exchange;

public class ExchangeDaoImpl extends AbstractDao implements ExchangeDao {

	private static Logger log = Logger.getLogger(ExchangeDaoImpl.class);
	
	public ExchangeDaoImpl() {
		super();
	}
	
	@Override
	public Exchange create(Exchange exchange) {

		SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
		.withSchemaName("iasion").withTableName("exchange")
		.usingGeneratedKeyColumns("id", "timestamp");

		MapSqlParameterSource namedParameters = new MapSqlParameterSource();

		try {
			String jsonMetadata = mapper.writeValueAsString(exchange.getMetadata());
			namedParameters.addValue("metadata", jsonMetadata, Types.OTHER);
			
			String jsonData = mapper.writeValueAsString(exchange.getData());
			namedParameters.addValue("data", jsonData, Types.OTHER);

		} catch (JsonGenerationException e) {
			log.error(e);
		} catch (JsonMappingException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}

		KeyHolder key = jdbcInsert.executeAndReturnKeyHolder(namedParameters);

		return this.get(Integer.valueOf(key.getKeys().get("id").toString()));
	}

	private Exchange get(Integer id) {
		String sql = "SELECT * FROM exchange WHERE id = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new ExchangeMapper(), id);
		} catch (Exception e) {
			log.error("get : id not found in base");
		}

		return null;
	}
	
	@Override
	public Exchange get(String guid) {
		String sql = "SELECT * FROM exchange WHERE metadata->>'guid' = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new ExchangeMapper(), guid);
		} catch (Exception e) {
			log.error("get : guid not found in base");
		}

		return null;
	}

	@Override
	public List<Exchange> get() {
		String sql = "SELECT * FROM exchange";
		return jdbcTemplate.query(sql, new ExchangeMapper());
	}

	@Override
	public void update(Exchange exchange) {
		
		String sql = "UPDATE exchange "
				+ "SET metadata=(:metadata)::jsonb, data=(:data)::jsonb "
				+ "WHERE id=:id";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		
		namedParameters.addValue("id", exchange.getId());
		
		try {

			String metadata = mapper.writeValueAsString(exchange.getMetadata());
			namedParameters.addValue("metadata", metadata, Types.OTHER);
			
			String data = mapper.writeValueAsString(exchange.getData());
			namedParameters.addValue("data", data, Types.OTHER);
			

		} catch (JsonGenerationException e) {
			log.error(e);
		} catch (JsonMappingException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}

		namedParameterJdbcTemplate.update(sql, namedParameters);
		
	}

	@Override
	public void delete(String guid) {
		
		String sql = "DELETE FROM exchange WHERE metadata->>'guid'=:guid";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("guid", guid);

		namedParameterJdbcTemplate.update(sql, namedParameters);
		
	}

}
