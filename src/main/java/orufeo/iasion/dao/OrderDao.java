package orufeo.iasion.dao;

import java.util.List;

import orufeo.iasion.data.objects.storage.Order;


public interface OrderDao {
	
	Order create(Order order);
	
	Order get(String guid);
	
	List<Order> get();
	
	void update(Order order);
	
	void delete(String guid);

}
