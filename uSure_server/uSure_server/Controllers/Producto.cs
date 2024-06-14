namespace uSure_server.Controllers;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;
using uSure_server.Controllers;

public class Producto
{
    [Key]
    public int ID { get; set; }
    public string Nombre { get; set; }
    public int Cantidad { get; set; }
    public int IDCategoria { get; set; }

    [JsonIgnore] // Ignora esta propiedad durante la serialización JSON
    public Categoria Categoria { get; set; }

    [JsonIgnore] // Ignora esta propiedad durante la serialización JSON
    public ICollection<GrupoProducto> GrupoProductos { get; set; }
}
