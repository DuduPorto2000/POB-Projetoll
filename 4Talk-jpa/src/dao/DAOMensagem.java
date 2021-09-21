package dao;

import java.util.List;

import javax.persistence.TypedQuery;

import modelo.Mensagem;


public class DAOMensagem extends DAO<Mensagem>{

	@SuppressWarnings("unchecked")
	public Mensagem read(Object chave) {
		try {
			int id = (Integer) chave;
			TypedQuery<Mensagem> q = (TypedQuery<Mensagem>) manager.createQuery("select m from Mensagem m where m.id = :x");
			q.setParameter("x",id);
			Mensagem mensagem = q.getSingleResult();
			return mensagem;
		}catch(Exception e) {
			return null;
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	public static List<Mensagem> queryMSGs(String termo) {
		TypedQuery<Mensagem> q = (TypedQuery<Mensagem>) manager.createQuery("select m from Mensagem m where m.texto like :x");
		q.setParameter("x","%"+ termo +"%");
		List<Mensagem> result = q.getResultList();
		if (result.size() > 0)
			return result;
		else
			return null;
	}
	
	@SuppressWarnings("unchecked")
	public Object obterUltimoId() {
		try {
			TypedQuery<Mensagem> q = (TypedQuery<Mensagem>) manager.createQuery("select m from Mensagem m order by m.id desc");
			Mensagem temp = q.getSingleResult();
			return (int) temp.getId();
		}catch(Exception e) {
			System.out.println("é aqui o erro");
			return null;
		}
	}
}
