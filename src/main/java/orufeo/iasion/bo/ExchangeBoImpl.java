package orufeo.iasion.bo;

import java.util.List;

import lombok.Setter;
import orufeo.iasion.dao.ExchangeDao;
import orufeo.iasion.data.objects.storage.Exchange;

public class ExchangeBoImpl implements ExchangeBo {

	@Setter private ExchangeDao exchangeDao;
	
	@Override
	public Exchange create(Exchange exchange) {
		
		return exchangeDao.create(exchange);
	}

	@Override
	public Exchange get(String guid) {
		
		return exchangeDao.get(guid);
	}

	@Override
	public List<Exchange> get() {
	
		return exchangeDao.get();
	}

	@Override
	public void update(Exchange exchange) {
		
		exchangeDao.update(exchange);
		
	}

	@Override
	public void delete(String guid) {
		
		exchangeDao.delete(guid);
		
	}
	
	

}
