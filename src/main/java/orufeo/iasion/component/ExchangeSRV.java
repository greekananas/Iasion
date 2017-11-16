package orufeo.iasion.component;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.mule.api.annotations.param.Payload;

import lombok.Setter;
import orufeo.iasion.bo.ExchangeBo;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.data.objects.storage.Exchange;
import orufeo.iasion.data.objects.storage.ExchangeMetadata;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.utils.CookiesValueTL;

public class ExchangeSRV {
	
	@Setter private ExchangeBo exchangeBo;
	@Setter private UserAccountBo userAccountBo;
	
	public Exchange get(@Payload Map<String, String> args) {

		String guid = args.get("guid");

		return exchangeBo.get(guid);

	}

	public List<Exchange> getAll(@Payload Map<String, String> args) {

		return exchangeBo.get();

	}

	public Exchange create(@Payload Exchange exchange) {

		Map<String, String> cookies = CookiesValueTL.get();
		UserAccount user = userAccountBo.getByToken(cookies.get("token"));
		ExchangeMetadata metadata = new ExchangeMetadata(user);

		exchange.setMetadata(metadata);
		
		return exchangeBo.create(exchange);

	}

	public void update(@Payload Exchange exchange) {

		Map<String, String> cookies = CookiesValueTL.get();
		UserAccount user = userAccountBo.getByToken(cookies.get("token"));
		
		exchange.getMetadata().setModificationDate(new Date());
		exchange.getMetadata().setLastModifierGuid(user.getMetadata().getGuid());
		
		exchangeBo.update(exchange);

	}

	public void delete(@Payload Map<String, String> args) {

		String guid = args.get("guid");

		exchangeBo.delete(guid);

	}

}
