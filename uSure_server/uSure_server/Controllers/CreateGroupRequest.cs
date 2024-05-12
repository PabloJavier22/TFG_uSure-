namespace uSure_server.Controllers
{
        public class CreateGroupRequest
        {
            public string Codigo { get; set; } // Código del grupo
            public string Nombre { get; set; } // Nombre del grupo

        public List<Guid> Usuarios { get; set; }
    }
}