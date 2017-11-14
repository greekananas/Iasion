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

import orufeo.iasion.data.objects.storage.WalletHistory;

public class WalletHistoryDaoImpl extends AbstractDao implements WalletHistoryDao  {

	private static Logger log = Logger.getLogger(WalletHistoryDaoImpl.class);

	public WalletHistoryDaoImpl() {
		super();
	}
	
	@Override
	public WalletHistory create(WalletHistory walletHistory) {
		SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
				.withSchemaName("iasion").withTableName("wallet_history")
				.usingGeneratedKeyColumns("id", "timestamp");

				MapSqlParameterSource namedParameters = new MapSqlParameterSource();

				try {
					String jsonMetadata = mapper.writeValueAsString(walletHistory.getMetadata());
					namedParameters.addValue("metadata", jsonMetadata, Types.OTHER);
					
					String jsonData = mapper.writeValueAsString(walletHistory.getData());
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

	private WalletHistory get(Integer id) {
		String sql = "SELECT * FROM wallet_history WHERE id = ?";

		try {
			return jdbcTemplate.queryForObject(sql, new WalletHistoryMapper(), id);
		} catch (Exception e) {
			log.error("get : id not found in base");
		}

		return null;
	}

	@Override
	public List<WalletHistory> get(String walletGuid) {
		String sql = "SELECT * FROM wallet_history where data->>'walletGuid'=?";
		return jdbcTemplate.query(sql, new WalletHistoryMapper(), walletGuid);
	}

}
