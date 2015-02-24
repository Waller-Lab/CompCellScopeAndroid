



% Define a simple 3D distance macro
calcDistance = @(x1,y1,z1,x2,y2,z2) sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2)+(z1-z2)*(z1-z2));
 
domeRadius = 60;    % LED Dome Radius, mm
LEDPitch  = 3.5;    % LED Pitch, mm
domeAngle  = 77;    % Full angle of LED coverage on dome, deg

% Options
saveOutput = 1;      % Toggles saving a coordinates file via file prompt
makeCircular = 1;    % Truncates the point cloud to a circle
calcPCBSpacing = 0;  % Calculate optimum PCB Spacing for the given pattern

seperationAngle = 2*asin(LEDPitch/(2*domeRadius));

radius = domeRadius/1000;  % radius of mounting flange in meters
domeAngle = domeAngle * (pi/180); % Convert to Radians

% Here we employ a hexagonal packing algorithm. There are more efficent
% ways to do this that will work over an entire sphere, but this works well
% enough for our application.

% These are to tune the absolute angular spacing to achieve the desired
% packing
phiCorrection = 1.17; 
thetaCorrection = 1.123;

% These constants control the "bending" of the array pattern around the
% dome, they help conce
phicons = .24;
thetacons = .29;

% These define the min and maximum cutoffs. They are used for error
% checking and have no effect on the array generation.
minCutoff = 3.3 / 1000; % in mm, converted to m
maxCutoff = 4.0 / 1000; % in mm, converted to m

% END USER VARIABLES

phispac = phiCorrection * seperationAngle; % row spacing
thetaspac = thetaCorrection * seperationAngle; % column spacing

rows = floor(2*domeAngle/thetaspac);
cols = floor(2*domeAngle/phispac);

sphericalCoords = [];
cartesianCoords = [];
rowCount = [];
distList = [];

for rowIdx=0:rows
    thinc=(2*(rowIdx-rows/2)/rows);
    thetaVal = thetaspac*((rowIdx-rows/2)); % determines the row we're on
    thetaVal=thetaspac*((rowIdx-rows/2)+abs(thinc)*thetacons)/(1+thetacons);
    phinsp=phispac*(1-phispac)+phicons*phispac*(2*abs(rowIdx-rows/2)/rows);
    
    cs = cols + mod(rowIdx,2);              % stagger each row

    thv2 = repmat(thetaVal,[cs+1 1]);       % builds the row index
    colVec = ( -cs/2 : cs/2 );              % creats array of columns to make the row
    phiv = colVec * phinsp;                % builds the row
    rc = [ phiv.' thv2 radius*ones(cs+1,1)];                   % row, column
    rn=zeros(cs+1,3);                       % converts to xyz
    R = zeros(cs+1,3);
    
    [zo,yo,xo] = sph2cart(rc(:,1),rc(:,2),1); % Cartesian coords if R = 1
    [zr,yr,xr] = sph2cart(rc(:,1),rc(:,2),rc(:,3)); % Cartesian coords if R = radius
    
    rc_phi = rc(:,1);
    rc_theta = rc(:,2);
    if (makeCircular)
        circr = domeAngle/2;
        xr=xr(zo>sqrt(1-circr*circr),:); % make circular
        yr=yr(zo>sqrt(1-circr*circr),:); % make circular
        zr=zr(zo>sqrt(1-circr*circr),:); % make circular
        rc_phi=rc_phi(zo>sqrt(1-circr*circr),:); % make circular
        rc_theta=rc_theta(zo>sqrt(1-circr*circr),:); % make circular
    end
    rowCount = [rowCount; size(xr,1)];
    sphericalCoords = [sphericalCoords; [rc_phi rc_theta]];
    cartesianCoords = [cartesianCoords; [xr yr zr]];
end


scatter3(cartesianCoords(:,1),cartesianCoords(:,2),cartesianCoords(:,3),100); % Swap X and Z for display - Solidworks coordinates are different than MATLAB's in the way I like to set things up (zp)
xlabel('x'); ylabel('y'); zlabel('z');

% Build the Array for importing into Solidworks or Inventor, prompt the
% user to save the file

R = vertcat([0,0,size(cartesianCoords,1)],cartesianCoords); % Add one row of zeros for compatability, plus number of holes in the last slot
if (saveOutput)
   [filename, pathname] = uiputfile('*.cor');
   dlmwrite(strcat(pathname,filename),R,'precision',16,'newline','pc');
   save('ccs_domeHoleCoordinates.mat','cartesianCoords','sphericalCoords')
end

% Calculates the optimum angles for placing the fanned controller PCBs to
% ensure it's possible to solder all LEDs
if (calcPCBSpacing)
   holeCount = size(R,1)-1;
   ledsPerBoard = 64;
   nBoards = ceil(holeCount/ledsPerBoard); % add one because spacing on the ends of the array will be sparse
   
   % now calculate the LED's per board including the empty sockets, so we
   % have equal spacing:
   newLEDsPerBoard = ceil(holeCount/nBoards);
   boardAngles = zeros(nBoards,1);
   ax1 = 1;
   ax2 = 3;
   holeAngles = atand(cartesianCoords(:,ax1)./cartesianCoords(:,ax2)); % get the angle of each hole across the board axis
   % Account for sign difference due to the tangent so we can step through 
   % posAngles = [ 90.- (-1.*holeAngles(holeAngles<0,:))+90; holeAngles(holeAngles>=0,:) ];
   
   centerHole = ceil(holeCount/2);
   % If we have an odd number of boards, place one in the center (90 deg)
   if(mod(nBoards,2)) % Odd number of boards
       boardAngles(ceil(nBoards/2)) = 0;
       posHole = centerHole+newLEDsPerBoard;
       negHole = centerHole-newLEDsPerBoard;
       posBoard = ceil(nBoards/2);
       negBoard = floor(nBoards/2);
   else % Even number of boards
       posHole = centerHole+floor(newLEDsPerBoard/2);
       negHole = centerHole-floor(newLEDsPerBoard/2);
       posBoard = nBoards/2 + 1;
       negBoard = nBoards/2;
   end
       
   while (negBoard > 0)
       boardAngles(negBoard) =  holeAngles(negHole);
       negHole = negHole - newLEDsPerBoard;
       negBoard = negBoard - 1;
   end

   while (posBoard <= nBoards)
       boardAngles(posBoard) =  holeAngles(posHole);
       posHole = posHole + newLEDsPerBoard;
       posBoard = posBoard + 1;
   end
       
   boardAngles
end
disp(horzcat(num2str(size(R,1)-1),' holes generated. '));
