package dao;

import java.util.List;

import javax.persistence.TypedQuery;

import modelo.Mensagem;


public class DAOMensagem extends DAO<Mensagem>{

	@SuppressWarnings("unchecked")
	public Mensagem read(Object chave) {
		int id = (Integer) chave;
		TypedQuery<Mensagem> q = (TypedQuery<Mensagem>) manager.createQuery("select m from Mensagem m where m.id = :x");
		q.setParameter("x",id);
		Mensagem mensagem = q.getSingleResult();
		return mensagem;
	}
	
	@SuppressWarnings({ "unchecked" })
	public static List<Mensagem> queryMSGs(String termo) {
		TypedQuery<Mensagem> q = (TypedQuery<Mensagem>) manager.createQuery("select m from Mensagem m where m.texto like :x");
		q.setParameter("x", termo);
		List<Mensagem> result = q.getResultList();
		if (result.size() > 0)
			return result;
		else
			return null;
	}
}
