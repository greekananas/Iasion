package orufeo.iasion.dao;

import java.util.List;

import orufeo.iasion.data.objects.storage.Exchange;


public interface ExchangeDao {
	
	Exchange create(Exchange exchange);
	
	Exchange get(String guid);
	
	List<Exchange> get();
	
	void update(Exchange exchange);
	
	void delete(String guid);

}
