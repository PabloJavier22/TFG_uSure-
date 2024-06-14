namespace uSure_server.Controllers;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;
using uSure_server.Controllers;

public class Categoria
{
    [Key]
    public int ID { get; set; }
    public string Nombre { get; set; }
    public int IDGrupo { get; set; }

    [JsonIgnore]
    public Grupo Grupo { get; set; }

    [JsonIgnore] // Ignora esta propiedad durante la serialización JSON
    public ICollection<Producto>? Productos { get; set; }
}
