package dao;

import java.util.List;

import javax.persistence.TypedQuery;

import modelo.Log;

public class DAOLog extends DAO<Log>{

	@SuppressWarnings("unchecked")
	public Log read(Object chave) {
		String nome =(String) chave;
		TypedQuery<Log> q = (TypedQuery<Log>) manager.createQuery("select l from Log l where l.nome like :x");
		q.setParameter("x", nome);
		List<Log> result = q.getResultList();
		if (result.size() > 0)
			return result.get(0);
		else
			return null;
	}

}
