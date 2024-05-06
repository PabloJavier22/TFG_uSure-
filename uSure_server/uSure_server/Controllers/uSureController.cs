using Microsoft.AspNetCore.Identity.Data;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using uSure_server;

namespace uSure_server.Controllers
{
    [ApiController]
    [Route("[controller]")]
    public class uSureController : ControllerBase
    {
        private readonly ApplicationDbContext _context;
        private readonly ILogger<uSureController> _logger;

        public uSureController(ILogger<uSureController> logger, ApplicationDbContext context)
        {
            _logger = logger;
            _context = context;
        }

        [HttpPost("Login")]
        public async Task<IActionResult> Login(LoginRequest request)
        {
            var user = await _context.Usuarios.FirstOrDefaultAsync(u => u.Nombre == request.Nombre && u.Password == request.Password);

            if (user == null)
            {
                return BadRequest("Nombre de usuario o contraseña incorrectos");
            }

            return Ok(user);
        }

        [HttpGet("UserList")]
        public async Task<IEnumerable<Usuario>> Get()
        {
            return await _context.Usuarios.ToListAsync();
        }

        [HttpPost("Register")]
        public async Task<IActionResult> Register(RegisterRequest request)
        {
            var existingUser = await _context.Usuarios.FirstOrDefaultAsync(u => u.Nombre == request.Nombre || u.Email == request.Email);

            if (existingUser != null)
            {
                return BadRequest("Ya existe un usuario con el mismo nombre o correo electrónico");
            }

    
            var newUser = new Usuario
            {
                Nombre = request.Nombre,
                Email = request.Email,
                Password = request.Password,
                UID = Guid.NewGuid() 
            };

       
            _context.Usuarios.Add(newUser);
            await _context.SaveChangesAsync();


          
            var homeGroup = new Grupo
            {
                Codigo = "Home",
                Miembros = newUser.UID.ToString() 
            };



            _context.Grupos.Add(homeGroup);
            await _context.SaveChangesAsync();


            var newCategory = new Categoria
            {
                Nombre = "newUSER",
                ID_Grupo = homeGroup.ID 
            };

            _context.Categorias.Add(newCategory);
            await _context.SaveChangesAsync();

            return Ok(newUser);
        }



    }
}
