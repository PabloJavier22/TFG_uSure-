﻿using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;

namespace uSure_server.Controllers
{
    public class Usuario
    {
        [Key]
        public Guid UID { get; set; }
        public string Nombre { get; set; }
        public string Email { get; set; }
        public string Password { get; set; }
        public ICollection<UsuarioGrupo> UsuarioGrupo { get; set; }
    }
}
