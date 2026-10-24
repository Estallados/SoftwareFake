package co.edu.unbosque.marketplace.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.unbosque.marketplace.entity.Resena;
import co.edu.unbosque.marketplace.entity.Usuario;

@Service
public class ResenaService implements CRUDOperation<Resena> {

	@Override
	public int create(Resena data) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int deleteById(Long id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Resena> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long count() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean exist(Long id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int updateById(Long id, Resena newData) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Resena getById(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean findUsernameAlreadyTaken(Usuario newUser) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean encontrarCorreoExitente(String username) {
		// TODO Auto-generated method stub
		return false;
	}

}
