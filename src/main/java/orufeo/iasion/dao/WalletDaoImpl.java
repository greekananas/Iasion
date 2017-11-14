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

import orufeo.iasion.data.objects.storage.Wallet;

public class WalletDaoImpl extends AbstractDao implements WalletDao {

	private static Logger log = Logger.getLogger(WalletDaoImpl.class);

	public WalletDaoImpl() {
		super();
	}

	@Override
	public Wallet create(Wallet wallet) {
		SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
				.withSchemaName("iasion").withTableName("wallet")
				.usingGeneratedKeyColumns("id", "timestamp");

				MapSqlParameterSource namedParameters = new MapSqlParameterSource();

				try {
					String jsonMetadata = mapper.writeValueAsString(wallet.getMetadata());
					namedParameters.addValue("metadata", jsonMetadata, Types.OTHER);
					
					String jsonData = mapper.writeValueAsString(wallet.getData());
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

	private Wallet get(Integer id) {
		String sql = "SELECT * FROM wallet WHERE id = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new WalletMapper(), id);
		} catch (Exception e) {
			log.error("get : id not found in base");
		}

		return null;
	}
	
	@Override
	public Wallet get(String guid) {
		String sql = "SELECT * FROM wallet WHERE metadata->>'guid' = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new WalletMapper(), guid);
		} catch (Exception e) {
			log.error("get : guid not found in base");
		}

		return null;
	}

	@Override
	public List<Wallet> get() {
		String sql = "SELECT * FROM wallet";
		return jdbcTemplate.query(sql, new WalletMapper());
	}

	@Override
	public void update(Wallet wallet) {
		String sql = "UPDATE wallet "
				+ "SET metadata=(:metadata)::jsonb, data=(:data)::jsonb "
				+ "WHERE id=:id";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		
		namedParameters.addValue("id", wallet.getId());
		
		try {

			String metadata = mapper.writeValueAsString(wallet.getMetadata());
			namedParameters.addValue("metadata", metadata, Types.OTHER);
			
			String data = mapper.writeValueAsString(wallet.getData());
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
		String sql = "DELETE FROM wallet WHERE metadata->>'guid'=:guid";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("guid", guid);

		namedParameterJdbcTemplate.update(sql, namedParameters);

	}

}
