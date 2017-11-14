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

import orufeo.iasion.data.objects.storage.Order;

public class OrderDaoImpl extends AbstractDao implements OrderDao{

	private static Logger log = Logger.getLogger(OrderDaoImpl.class);
	
	public OrderDaoImpl() {
		super();
	}
	
	@Override
	public Order create(Order order) {
		
		SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
		.withSchemaName("iasion").withTableName("order")
		.usingGeneratedKeyColumns("id", "timestamp");

		MapSqlParameterSource namedParameters = new MapSqlParameterSource();

		try {
			String jsonMetadata = mapper.writeValueAsString(order.getMetadata());
			namedParameters.addValue("metadata", jsonMetadata, Types.OTHER);
			
			String jsonData = mapper.writeValueAsString(order.getData());
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
	
	private Order get(Integer id) {
		String sql = "SELECT * FROM order WHERE id = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new OrderMapper(), id);
		} catch (Exception e) {
			log.error("get : id not found in base");
		}

		return null;
	}

	@Override
	public Order get(String guid) {
		String sql = "SELECT * FROM order WHERE metadata->>'guid' = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new OrderMapper(), guid);
		} catch (Exception e) {
			log.error("get : guid not found in base");
		}

		return null;
	}

	@Override
	public List<Order> get() {
		String sql = "SELECT * FROM order";
		return jdbcTemplate.query(sql, new OrderMapper());
	}

	@Override
	public void update(Order order) {
		String sql = "UPDATE order "
				+ "SET metadata=(:metadata)::jsonb, data=(:data)::jsonb "
				+ "WHERE id=:id";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		
		namedParameters.addValue("id", order.getId());
		
		try {

			String metadata = mapper.writeValueAsString(order.getMetadata());
			namedParameters.addValue("metadata", metadata, Types.OTHER);
			
			String data = mapper.writeValueAsString(order.getData());
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
		
		String sql = "DELETE FROM order WHERE metadata->>'guid'=:guid";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("guid", guid);

		namedParameterJdbcTemplate.update(sql, namedParameters);
		
	}

}
