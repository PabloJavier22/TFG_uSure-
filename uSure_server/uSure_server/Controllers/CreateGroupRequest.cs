namespace uSure_server.Controllers
{
        public class CreateGroupRequest
        {
            public string Codigo { get; set; } 
            public string Nombre { get; set; } 

        public List<Guid> Usuarios { get; set; }
    }
}