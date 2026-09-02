package com.agendamentos.equadras.security;

import com.agendamentos.equadras.model.enums.Role;

public record UsuarioAutenticado(Long id, Role role) {}