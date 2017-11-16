package orufeo.iasion.component;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.mule.api.annotations.param.Payload;

import lombok.Setter;
import orufeo.iasion.bo.OrderBo;
import orufeo.iasion.bo.UserAccountBo;
import orufeo.iasion.data.objects.storage.Order;
import orufeo.iasion.data.objects.storage.OrderMetadata;
import orufeo.iasion.data.objects.storage.UserAccount;
import orufeo.iasion.data.objects.storage.WalletMetadata;
import orufeo.iasion.utils.CookiesValueTL;

public class OrderSRV {

	@Setter private OrderBo orderBo;
	@Setter private UserAccountBo userAccountBo;

	public Order get(@Payload Map<String, String> args) {

		String guid = args.get("guid");

		return orderBo.get(guid);

	}

	public List<Order> getAll(@Payload Map<String, String> args) {

		return orderBo.get();

	}

	public Order create(@Payload Order order) {

		Map<String, String> cookies = CookiesValueTL.get();
		UserAccount user = userAccountBo.getByToken(cookies.get("token"));
		OrderMetadata metadata = new OrderMetadata(user);

		order.setMetadata(metadata);
		
		return orderBo.create(order);

	}

	public void update(@Payload Order order) {
		Map<String, String> cookies = CookiesValueTL.get();
		UserAccount user = userAccountBo.getByToken(cookies.get("token"));
		
		order.getMetadata().setModificationDate(new Date());
		order.getMetadata().setLastModifierGuid(user.getMetadata().getGuid());
		
		orderBo.update(order);

	}

	public void delete(@Payload Map<String, String> args) {

		String guid = args.get("guid");

		orderBo.delete(guid);

	}
	
}
