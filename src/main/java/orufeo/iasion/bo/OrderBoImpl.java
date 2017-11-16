package orufeo.iasion.bo;

import java.util.List;

import lombok.Setter;
import orufeo.iasion.dao.OrderDao;
import orufeo.iasion.data.objects.storage.Order;

public class OrderBoImpl implements OrderBo {

	@Setter private OrderDao orderDao;
	
	@Override
	public Order create(Order order) {
		
		return orderDao.create(order);
	}

	@Override
	public Order get(String guid) {
		return orderDao.get(guid);
	}

	@Override
	public List<Order> get() {
		
		return orderDao.get();
	}

	@Override
	public void update(Order order) {
		orderDao.update(order);
		
	}

	@Override
	public void delete(String guid) {
		orderDao.delete(guid);
		
	}

}
