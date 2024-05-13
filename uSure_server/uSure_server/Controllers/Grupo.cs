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
        public ICollection<UsuarioGrupo> Usuarios { get; set; } 
        public ICollection<Categoria> Categorias { get; set; }
        public ICollection<GrupoProducto> GrupoProductos { get; set; }
    }
}
