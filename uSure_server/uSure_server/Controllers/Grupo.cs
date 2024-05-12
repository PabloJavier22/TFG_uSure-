using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace uSure_server.Controllers
{
    public class Grupo
    {
        [Key]
        public int ID { get; set; }
        public string Codigo { get; set; }
        public string Nombre { get; set; }

        // Propiedad Usuarios
        public ICollection<UsuarioGrupo> Usuarios { get; set; } // Suponiendo que un grupo pueda tener varios usuarios
        // Fin de la propiedad Usuarios

        public ICollection<Categoria> Categorias { get; set; }
        public ICollection<GrupoProducto> GrupoProductos { get; set; }
    }
}
