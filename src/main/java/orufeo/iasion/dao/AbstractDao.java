package orufeo.iasion.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import lombok.Getter;
import lombok.Setter;
import orufeo.iasion.data.objects.storage.Exchange;
import orufeo.iasion.data.objects.storage.ExchangeData;
import orufeo.iasion.data.objects.storage.ExchangeMetadata;
import orufeo.iasion.data.objects.storage.Order;
import orufeo.iasion.data.objects.storage.OrderData;
import orufeo.iasion.data.objects.storage.OrderMetadata;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.UserAccountData;
import orufeo.iasion.data.objects.storage.UserAccountMetadata;
import orufeo.iasion.data.objects.storage.Wallet;
import orufeo.iasion.data.objects.storage.WalletData;
import orufeo.iasion.data.objects.storage.WalletHistory;
import orufeo.iasion.data.objects.storage.WalletHistoryData;
import orufeo.iasion.data.objects.storage.WalletHistoryMetadata;
import orufeo.iasion.data.objects.storage.WalletMetadata;

/**
 * @author chenav
 *
 */
public abstract class AbstractDao {

	@Getter	@Setter	protected DataSourceTransactionManager transactionManager;
	@Getter	@Setter	protected JdbcTemplate jdbcTemplate;
	@Getter	@Setter	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Getter	@Setter	protected ObjectMapper mapper;

	protected class UserAccountMapper implements RowMapper<UserAccount> {
		@Override
		public UserAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				UserAccount user = new UserAccount();

				user.setId(rs.getInt("id"));
				user.setTimestamp(rs.getTimestamp("timestamp"));

				String metadataJSON = rs.getString("metadata");
				if (null != metadataJSON) {
					UserAccountMetadata metadata = mapper.readValue(metadataJSON, UserAccountMetadata.class);
					user.setMetadata(metadata);
				}


				String dataJSON = rs.getString("data");
				if (null != dataJSON) {
					UserAccountData data = mapper.readValue(dataJSON, UserAccountData.class);
					user.setData(data);
				} 

				return user;

			} catch (Exception e) {
				throw new RuntimeException("Erreur dans le UserAccount mapping:"+ e.getMessage());
			}


		}
	}
	
	protected class WalletMapper implements RowMapper<Wallet> {
		@Override
		public Wallet mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				Wallet wallet = new Wallet();

				wallet.setId(rs.getInt("id"));
				wallet.setTimestamp(rs.getTimestamp("timestamp"));

				String metadataJSON = rs.getString("metadata");
				if (null != metadataJSON) {
					WalletMetadata metadata = mapper.readValue(metadataJSON, WalletMetadata.class);
					wallet.setMetadata(metadata);
				}


				String dataJSON = rs.getString("data");
				if (null != dataJSON) {
					WalletData data = mapper.readValue(dataJSON, WalletData.class);
					wallet.setData(data);
				} 

				return wallet;

			} catch (Exception e) {
				throw new RuntimeException("Erreur dans le Wallet mapping:"+ e.getMessage());
			}

		}
	}
	
	protected class WalletHistoryMapper implements RowMapper<WalletHistory> {
		@Override
		public WalletHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				WalletHistory walletHistory = new WalletHistory();

				walletHistory.setId(rs.getInt("id"));
				walletHistory.setTimestamp(rs.getTimestamp("timestamp"));

				String metadataJSON = rs.getString("metadata");
				if (null != metadataJSON) {
					WalletHistoryMetadata metadata = mapper.readValue(metadataJSON, WalletHistoryMetadata.class);
					walletHistory.setMetadata(metadata);
				}


				String dataJSON = rs.getString("data");
				if (null != dataJSON) {
					WalletHistoryData data = mapper.readValue(dataJSON, WalletHistoryData.class);
					walletHistory.setData(data);
				} 

				return walletHistory;

			} catch (Exception e) {
				throw new RuntimeException("Erreur dans le WalletHistory mapping:"+ e.getMessage());
			}

		}
	}

	protected class ExchangeMapper implements RowMapper<Exchange> {
		@Override
		public Exchange mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				Exchange exchange = new Exchange();

				exchange.setId(rs.getInt("id"));
				exchange.setTimestamp(rs.getTimestamp("timestamp"));

				String metadataJSON = rs.getString("metadata");
				if (null != metadataJSON) {
					ExchangeMetadata metadata = mapper.readValue(metadataJSON, ExchangeMetadata.class);
					exchange.setMetadata(metadata);
				}


				String dataJSON = rs.getString("data");
				if (null != dataJSON) {
					ExchangeData data = mapper.readValue(dataJSON, ExchangeData.class);
					exchange.setData(data);
				} 

				return exchange;

			} catch (Exception e) {
				throw new RuntimeException("Erreur dans le Exchange mapping:"+ e.getMessage());
			}

		}
	}
	
	protected class OrderMapper implements RowMapper<Order> {
		@Override
		public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				Order order = new Order();

				order.setId(rs.getInt("id"));
				order.setTimestamp(rs.getTimestamp("timestamp"));

				String metadataJSON = rs.getString("metadata");
				if (null != metadataJSON) {
					OrderMetadata metadata = mapper.readValue(metadataJSON, OrderMetadata.class);
					order.setMetadata(metadata);
				}


				String dataJSON = rs.getString("data");
				if (null != dataJSON) {
					OrderData data = mapper.readValue(dataJSON, OrderData.class);
					order.setData(data);
				} 

				return order;

			} catch (Exception e) {
				throw new RuntimeException("Erreur dans le Order mapping:"+ e.getMessage());
			}

		}
	}
	
	
	public void init() {
		DataSource dataSource = this.transactionManager.getDataSource();
		jdbcTemplate = new JdbcTemplate(dataSource);
		namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
	}

	

}
