namespace uSure_server.Controllers
{
    public class ProductoCreateRequest
    {
        public string Nombre { get; set; }
        public int Cantidad { get; set; }
        public int IDCategoria { get; set; }
        public List<GrupoProductoCreateRequest> GrupoProductos { get; set; }
    }
}