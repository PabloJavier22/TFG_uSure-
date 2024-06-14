namespace uSure_server.Controllers;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;
using uSure_server.Controllers;

public class Grupo
{
    [Key]
    public int ID { get; set; }
    public string Codigo { get; set; }
    public string Nombre { get; set; }

    public ICollection<UsuarioGrupo> Usuarios { get; set; }

    [JsonIgnore] // Ignora esta propiedad durante la serialización JSON
    public ICollection<Categoria> Categorias { get; set; }

    [JsonIgnore] // Ignora esta propiedad durante la serialización JSON
    public ICollection<GrupoProducto> GrupoProductos { get; set; }
}
