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

import orufeo.iasion.data.objects.storage.UserAccount;

public class UserAccountDaoImpl extends AbstractDao implements UserAccountDao {

	private static Logger log = Logger.getLogger(UserAccountDaoImpl.class);
	
	public UserAccountDaoImpl() {
		super();
	}
	
	@Override
	public UserAccount create(UserAccount userAccount) {
		SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
				.withSchemaName("iasion").withTableName("useraccount")
				.usingGeneratedKeyColumns("id", "timestamp");

				MapSqlParameterSource namedParameters = new MapSqlParameterSource();

				try {
					String jsonMetadata = mapper.writeValueAsString(userAccount.getMetadata());
					namedParameters.addValue("metadata", jsonMetadata, Types.OTHER);
					
					String jsonData = mapper.writeValueAsString(userAccount.getData());
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

	private UserAccount get(Integer id) {
		String sql = "SELECT * FROM useraccount WHERE id = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new UserAccountMapper(), id);
		} catch (Exception e) {
			log.error("get : id not found in base");
		}

		return null;
	}
	
	@Override
	public UserAccount get(String guid) {
		String sql = "SELECT * FROM useraccount WHERE metadata->>'guid' = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new UserAccountMapper(), guid);
		} catch (Exception e) {
			log.error("get : guid not found in base");
		}

		return null;
	}

	@Override
	public List<UserAccount> get() {
		String sql = "SELECT * FROM useraccount";
		return jdbcTemplate.query(sql, new UserAccountMapper());
	}

	@Override
	public void update(UserAccount userAccount) {
		String sql = "UPDATE useraccount "
				+ "SET metadata=(:metadata)::jsonb, data=(:data)::jsonb "
				+ "WHERE id=:id";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		
		namedParameters.addValue("id", userAccount.getId());
		
		try {

			String metadata = mapper.writeValueAsString(userAccount.getMetadata());
			namedParameters.addValue("metadata", metadata, Types.OTHER);
			
			String data = mapper.writeValueAsString(userAccount.getData());
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
		
		String sql = "DELETE FROM useraccount WHERE metadata->>'guid'=:guid";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource();
		namedParameters.addValue("guid", guid);

		namedParameterJdbcTemplate.update(sql, namedParameters);
		
	}

}
