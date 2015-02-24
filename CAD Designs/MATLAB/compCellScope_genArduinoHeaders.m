close all; clear all;

% This file contains the hole - channel mappings. Channels are defined as
% the position along the chain of LED controller chips, which are
% effectivly shift registers. Basically this tells which bit corresponds to
% a given LED, or "hole". The format is N rows with channel, hole. The
% first row is a header and is omitted.
channelMap=xlsread('channelMap.xlsx');

% Loads a .mat file containing two variables:
% - cartesianCoords contains the x,y,z coordinates of each hole in meters
% - sphericalCoords contains the theta_x, theta_y, radius in mm.
load('ccs_domeHoleCoordinates.mat');

% We define the darkfield as slightly larger than the actual NA so we
% ensure we don't have any LEDs illuminating in the brightfield region.
darkfieldNAOffset = 0.08;

minNA = 0.1;
maxNA = 0.6;
stepNA = 0.05;

% Number of TLC5926 LED controller chips in the chain
chain_ct = 36;

% show debug plots
debug=0;

% Width of the Annulus in units of NA
annulusOffset = 0.12;

for i=1:size(sphericalCoords,1)
   dists(i) = round(1000*sqrt(sin(sphericalCoords(i,1))*sin(sphericalCoords(i,1))+sin(sphericalCoords(i,2))* sin(sphericalCoords(i,2))));
end

outArray = horzcat(channelMap(:,1),channelMap(:,2),round(1000*sphericalCoords(:,1:2)),dists');

fid = fopen('arduinoCoords.h.output','w');

fprintf(fid,'// FORMAT: hole number,, channel, 1000*Phi, 1000*Theta, 1000*angle to z-axis\n'); % Header
fprintf(fid,'PROGMEM const int ledMap[%d][5] = {\n',size(sphericalCoords,1)); % Header
for i=1:size(sphericalCoords,1)-1
        fprintf(fid,'/*%3d*/ {%d, %d, %d, %d, %d}, \n', i, outArray(i,1), outArray(i,2), outArray(i,3), outArray(i,4), outArray(i,5));
end
fprintf(fid,'/*%3d*/ {%d, %d, %.d, %d, %d} \n',size(sphericalCoords,1),outArray(end,1),outArray(end,2),outArray(end,3),outArray(end,4),outArray(end,5)); % last value doesnt have a comma
fprintf(fid,'};'); % Footer
fclose(fid);

fid = fopen('arduinoCoords_cartesian.corMap','w');
fprintf(fid,'// FORMAT: hole number, x, y, z\n'); % Header
fprintf(fid,'public final double[][] domeCoordinates = new double[][]{\n'); % Header
for i=1:size(cartesianCoords,1)
        fprintf(fid,'/*%3d*/ {%.4f, %.4f, %.4f},\n', i, cartesianCoords(i,1),cartesianCoords(i,2),cartesianCoords(i,3));
end
fprintf(fid,'};'); % Footer
fclose(fid);


% Generate DPC patterns for range of NA's
%%
fid2 = fopen(horzcat('pattern_bitmaps.h'),'w');
fprintf(fid2,'//pattern_bitmaps.h\n');
fprintf(fid2,['//Generated ' date '\n\n']);
fprintf(fid2,'// List of valid NAs in this file:\n');
fprintf(fid2,['PROGMEM const double validNA[' num2str((maxNA-minNA)/stepNA +1) '] = {']);
for na=minNA:stepNA:(maxNA-stepNA)
    fprintf(fid2,[ num2str(na) ', ']);
end
fprintf(fid2,[ num2str(maxNA) '};\n\n']);

fprintf(fid2,'// This list of LED numbers was generated from berkeley.bmp');
fprintf(fid2,'\n');
fprintf(fid2,'PROGMEM const int calLogoHoles[152] = {59,60,61,62,63,64,72,76,77,78,79,80,81,82,83,84,90,91,94,95,96,97,98,99,100,101,102,103,104,110,111,113,114,121,122,123,124,131,132,134,144,145,146,152,153,156,167,168,174,175,178,179,180,190,191,195,196,198,199,200,201,202,203,212,213,217,218,219,221,222,225,226,232,236,241,242,245,250,256,259,264,265,266,268,269,270,271,274,281,282,287,288,290,291,293,294,295,296,309,310,311,313,319,332,333,337,353,354,358,359,360,361,362,363,364,374,375,377,378,379,380,381,382,383,384,385,386,394,395,396,397,402,405,406,407,413,414,415,422,426,427,432,441,442,445,446,460,461,462};');
fprintf(fid2,'\n\n');

na_idx = 1;
for na=minNA:stepNA:maxNA
    BFcount = 0;
    DFcount = 0;
    ANCount = 0;
    clear brightfieldList topList ANCoords annulusList topCoords bottomList bottomCoords rightList rightCoords leftList leftCoords darkfieldList
    na
    
% Generate list of brightfield LEDs inside a specified NA
minDist = inf;
minHole = -1;
for i=1:508
   if (sqrt(sin(sphericalCoords(i,1))*sin(sphericalCoords(i,1))+sin(sphericalCoords(i,2))*sin(sphericalCoords(i,2))) < na)
       BFcount=BFcount+1;
       brightfieldList(BFcount)=i;
       BFcoords(BFcount,1:2)=[sphericalCoords(i,1),sphericalCoords(i,2)];
   end
   
   if (sqrt(sin(sphericalCoords(i,1))*sin(sphericalCoords(i,1))+sin(sphericalCoords(i,2))*sin(sphericalCoords(i,2))) > (na + darkfieldNAOffset))
       DFcount=DFcount+1;
       darkfieldList(DFcount)=i;
       DFcoords(DFcount,1:2)=[sphericalCoords(i,1),sphericalCoords(i,2)];
   end
   
   % Annulus
   if ((sqrt(sin(sphericalCoords(i,1))*sin(sphericalCoords(i,1))+sin(sphericalCoords(i,2))*sin(sphericalCoords(i,2))) > (na + darkfieldNAOffset)) && (sqrt(sin(sphericalCoords(i,1))*sin(sphericalCoords(i,1))+sin(sphericalCoords(i,2))*sin(sphericalCoords(i,2))) < (na + darkfieldNAOffset + annulusOffset)))
       ANCount=ANCount+1;
       annulusList(ANCount)=i;
       ANcoords(ANCount,1:2)=[sphericalCoords(i,1),sphericalCoords(i,2)];
   end
   
end

[minVal, minHole] = min(abs(sqrt(sin(sphericalCoords(:,1)).*sin(sphericalCoords(:,1))+sin(sphericalCoords(:,2)).*sin(sphericalCoords(:,2))) - na));
   % Find Closest hole to circle defined by NA
   if (abs((sqrt(sin(sphericalCoords(i,1))*sin(sphericalCoords(i,1))+sin(sphericalCoords(i,2))*sin(sphericalCoords(i,2))) - na)) < minDist)
       minDist = sqrt(sin(sphericalCoords(i,1))*sin(sphericalCoords(i,1))+sin(sphericalCoords(i,2))*sin(sphericalCoords(i,2)));
       minHole = i;
   end
size(brightfieldList)

% If odd number, remove the led that's furthest away from center to make count even
if (mod(BFcount,2))
    for idx=1:BFcount
        hole = brightfieldList(idx);
        dists(idx)= sqrt(sin(sphericalCoords(hole,2))^2+sin(sphericalCoords(hole,1))^2);
    end
    [c,i]=max(dists);
    brightfieldList = brightfieldList(brightfieldList~=i);
    BFcount=BFcount-1;
end

% Divide into top/bottom pairs
topCount=0;
bottomCount=0;

for idx=1:BFcount
   hole=brightfieldList(idx);
   if(sphericalCoords(hole,2)>0)
       topCount=topCount+1;
       topList(topCount)=hole;
       topCoords(topCount,1:2)=[sphericalCoords(hole,1),sphericalCoords(hole,2)];
   elseif(sphericalCoords(hole,2)<0)
       bottomCount=bottomCount+1;
       bottomList(bottomCount)=hole;
       bottomCoords(bottomCount,1:2)=[sphericalCoords(hole,1),-sphericalCoords(hole,2)];
   end
%    elseif(sphericalCoords(hole,1)>0) % Deal with holes along the y-axis
%        topCount=topCount+1;
%        topList(topCount)=hole;
%        topCoords(topCount,1:2)=[sphericalCoords(hole,1),sphericalCoords(hole,2)];
%    else
%        bottomCount=bottomCount+1;
%        bottomList(bottomCount)=hole;
%        bottomCoords(bottomCount,1:2)=[sphericalCoords(hole,1),-sphericalCoords(hole,2)];
%    end
end

% Divide into left/right pairs
rightCount=0;
leftCount=0;

for idx=1:BFcount
    hole=brightfieldList(idx);
   if(sphericalCoords(hole,1)>0)
       rightCount=rightCount+1;
       rightList(rightCount)=hole;
       rightCoords(rightCount,1:2)=[sphericalCoords(hole,1),sphericalCoords(hole,2)];
   elseif(sphericalCoords(hole,1)<0)
       leftCount=leftCount+1;
       leftList(leftCount)=hole;
       leftCoords(leftCount,1:2)=[-sphericalCoords(hole,1),sphericalCoords(hole,2)];
       
   end
%    elseif (sphericalCoords(hole,2)<0) % Deal with holes along the x-axis
%        leftCount=leftCount+1;
%        leftList(leftCount)=hole;
%        leftCoords(leftCount,1:2)=[-sphericalCoords(hole,1),sphericalCoords(hole,2)];
%    else
%        rightCount=rightCount+1;
%        rightList(rightCount)=hole;
%        rightCoords(rightCount,1:2)=[sphericalCoords(hole,1),sphericalCoords(hole,2)];           
%    end
   
end

%%
% Calculate LED with closest center to NA band, between a certain range of
% angles
alignmentHoles = [];

minAngleFromAxis = 30;
for quadrant = 1:4

    anglesFromAxes = atand(abs(sphericalCoords(:,2))./abs(sphericalCoords(:,1)));
    suitableAngles = (anglesFromAxes > minAngleFromAxis) .* (anglesFromAxes < (90-minAngleFromAxis));
    suitableHoles = find(suitableAngles == 1);
    switch (quadrant)
        case 1 % Upper Right
            suitableHoles = suitableHoles(sphericalCoords(suitableHoles,1)>0);
            suitableHoles = suitableHoles(sphericalCoords(suitableHoles,2)>0);
        case 2 % Bottom Right
            suitableHoles = suitableHoles(sphericalCoords(suitableHoles,1)>0);
            suitableHoles = suitableHoles(sphericalCoords(suitableHoles,2)<0);
        case 3 % Bottom Left
            suitableHoles = suitableHoles(sphericalCoords(suitableHoles,1)<0);
            suitableHoles = suitableHoles(sphericalCoords(suitableHoles,2)<0);
        case 4 % Top Left
            suitableHoles = suitableHoles(sphericalCoords(suitableHoles,1)<0);
            suitableHoles = suitableHoles(sphericalCoords(suitableHoles,2)>0);

    end
    distsFromNA = abs(sqrt(sin(sphericalCoords(suitableHoles,1)).^2+sin(sphericalCoords(suitableHoles,2)).^2) - na);
    [minVal,pos] = min(distsFromNA);
    alignmentHoles = [alignmentHoles suitableHoles(pos)];
end

if (debug)
    figure; hold on;
    title('Before Filtering');
    subplot(221);
    scatter(rightCoords(:,1),rightCoords(:,2))
    title('right');
    xlim([-1,1])
    ylim([-1,1])
    subplot(222);
    scatter(leftCoords(:,1),leftCoords(:,2));
    title('left(flipped)');
    xlim([-1,1])
    ylim([-1,1])
    subplot(223)
    scatter(topCoords(:,1),topCoords(:,2));
    title('top');
    title('Annulus');
    xlim([-1,1])
    ylim([-1,1])
    subplot(224)
    scatter(bottomCoords(:,1),bottomCoords(:,2));
    title('bottom(flipped)');
    title('Annulus');
    xlim([-1,1])
    ylim([-1,1])
    hold off;
    
    figure; hold on;
    subplot(221);
    scatter(BFcoords(:,1),BFcoords(:,2))
    title('Brightfield');
    title('Annulus');
    xlim([-1,1])
    ylim([-1,1])
    subplot(222);
    scatter(DFcoords(:,1),DFcoords(:,2));
    title('Darkfield');
    title('Annulus');
    xlim([-1,1])
    ylim([-1,1])
    subplot(223)
    scatter(ANcoords(:,1),ANcoords(:,2));
    title('Annulus');
    xlim([-1,1])
    ylim([-1,1])
    subplot(224)
    scatter(sphericalCoords(alignmentHoles,1),(sphericalCoords(alignmentHoles,2)));
    title('Alignment');
    title('Annulus');
    xlim([-1,1])
    ylim([-1,1])
    hold off;
    
end

if (bottomCount ~= topCount)
    tb_difference = topCount-bottomCount;
    filteredTopList = topList;
    filteredBottomList = bottomList;
    filteredTopCoords = topCoords;
    filteredBottomCoords = bottomCoords;
    if (topCount>bottomCount)
        dists = sqrt(sin(topCoords(:,1)).*sin(topCoords(:,1))+sin(topCoords(:,2)).*sin(topCoords(:,2)));
        [b,i] = sort(dists);
        holesToRemove = topList(i(size(i,1)-abs(tb_difference):end));
        filteredTopList(i(size(i,1)-abs(tb_difference)+1:end)) = [];
        % Set the same Coordinates to zero
        filteredTopCoords(i(size(i,1)-abs(tb_difference)+1:end),:) = [];
    else
        dists = sqrt(sin(bottomCoords(:,1)).*sin(bottomCoords(:,1))+sin(bottomCoords(:,2)).*sin(bottomCoords(:,2)));
        [b,i] = sort(dists);
        holesToRemove = bottomList(i(size(i,1)-abs(tb_difference):end));
        filteredBottomList(i(size(i,1)-abs(tb_difference)+1:end)) = [];
        % Set the same Coordinates to zero
        filteredBottomCoords(i(size(i,1)-abs(tb_difference)+1:end),:) = [];
    end
    topList = filteredTopList;
    bottomList = filteredBottomList;
    topCoords = filteredTopCoords;
    bottomCoords = filteredBottomCoords;
end

if (rightCount ~= leftCount)
    lr_difference = rightCount-leftCount;
    filteredRightList = rightList;
    filteredLeftList = leftList;
    filteredRightCoords = rightCoords;
    filteredLeftCoords = leftCoords;
    if (rightCount>leftCount)
        dists = sqrt(sin(rightCoords(:,1)).*sin(rightCoords(:,1))+sin(rightCoords(:,2)).*sin(rightCoords(:,2)));
        [b,i] = sort(dists);
        holesToRemove = rightList(i(size(i,1)-abs(lr_difference):end));
        filteredRightList(i(size(i,1)-abs(lr_difference)+1:end)) = [];
        filteredRightCoords(i(size(i,1)-abs(lr_difference)+1:end),:) = [];
    else
        dists = sqrt(sin(leftCoords(:,1)).*sin(leftCoords(:,1))+sin(leftCoords(:,2)).*sin(leftCoords(:,2)));
        [b,i] = sort(dists);
        holesToRemove = leftList(i(size(i,1)-abs(lr_difference):end));
        filteredLeftList(i(size(i,1)-abs(lr_difference)+1:end)) = [];
        filteredLeftCoords(i(size(i,1)-abs(lr_difference)+1:end),:) = [];
    end
    rightList = filteredRightList;
    leftList = filteredLeftList;
    rightCoords = filteredRightCoords;
    leftCoords = filteredLeftCoords;
end

leftBits = uint16(zeros(chain_ct,1));
rightBits = uint16(zeros(chain_ct,1));
topBits = uint16(zeros(chain_ct,1));
bottomBits = uint16(zeros(chain_ct,1));
darkfieldBits = uint16(zeros(chain_ct,1));
brightfieldBits = uint16(zeros(chain_ct,1));
annulusBits = uint16(zeros(chain_ct,1));
alignmentBits = uint16(zeros(chain_ct,1));

fprintf(fid2,'// FORMAT: top, bottom, left, right, bf, darkfield, annulus, alignment\n'); % Header
fprintf(fid2,horzcat('PROGMEM const int naHoles',num2str(100*na),'[',num2str(chain_ct),'][8] = {\n')); % Header

for hole=1:508
    channel = channelMap(hole,2); % Correct - zero indexing (-1 is invalid)
    chip = uint16((floor(channel/16)+1)); % Correct - chip is one indexing
    bitNum = (channel-16*(chip-1))+1; % Correct - BitNum is one indexing
    
    if (channel ~= -1)
        if (ismember(hole,leftList)) % is this hole in leftList
            leftBits(chip,1) = bitset(leftBits(chip,1),bitNum);
        end

        if (ismember(hole,rightList))
            rightBits(chip,1) = bitset(rightBits(chip,1),bitNum);
        end

        if (ismember(hole,topList))
            topBits(chip,1) = bitset(topBits(chip,1),bitNum);
        end

        if (ismember(hole,bottomList))
            bottomBits(chip,1) = bitset(bottomBits(chip,1),bitNum);
        end

        if (ismember(hole,brightfieldList))
            brightfieldBits(chip,1) = bitset(brightfieldBits(chip,1),bitNum);
        end

        if (ismember(hole,darkfieldList))
            darkfieldBits(chip,1) = bitset(darkfieldBits(chip,1),bitNum);
        end
        
        if (ismember(hole,annulusList))
            annulusBits(chip,1) = bitset(annulusBits(chip,1),bitNum);
        end
        
        if (ismember(hole,alignmentHoles))
            alignmentBits(chip,1) = bitset(alignmentBits(chip,1),bitNum);
        end
    end
end
for j=1:chain_ct-1
    fprintf(fid2,'   {%d, %d, %d, %d, %d, %d, %d, %d}, \n',topBits(j,1),bottomBits(j,1),leftBits(j,1),rightBits(j,1),brightfieldBits(j,1),darkfieldBits(j,1),annulusBits(j,1),alignmentBits(j,1));
end
fprintf(fid2,'   {%d, %d, %d, %d, %d, %d, %d, %d} \n',topBits(end,1),bottomBits(end,1),leftBits(end,1),rightBits(end,1),brightfieldBits(end,1),darkfieldBits(end,1),annulusBits(end,1),alignmentBits(end,1)); % last value doesnt have a comma
fprintf(fid2,'};\n'); % Footer
fprintf(fid2,'\n'); % Footer



%% Brightfield LED scans go out of order based on the bit patterns
% Calculated above. This saves the corresponding 

brightfieldListOrdered = [];
for channel=1:(16*(chain_ct-1))
   chip = floor(channel/16)+1;

   if (bitget(brightfieldBits(chip),channel-16*(chip-1)+1))
       brightfieldListOrdered = [brightfieldListOrdered; [channel find(channelMap(:,2)==channel)]];
   end
end

globalBrightfieldList{na_idx}.na = na;
globalBrightfieldList{na_idx}.bf_imageOrder = brightfieldListOrdered;
na_idx = na_idx+1;


end
fclose(fid2);
%%
if (debug)
    figure; hold on;
    for i=1:508
        if ismember(i,annulusList)
            plot(cartesianCoords(i,3),cartesianCoords(i,2),'o');
        end
    end

    % Build the scatter plot with hole indicies
     %scatter3(cartesianCoords(:,3),cartesianCoords(:,2),zeros(size(cartesianCoords,1),1))
    % close all;
    figure(2);
    hold on;
    axis([min(sphericalCoords(:,1))-.01, max(sphericalCoords(:,1))+.01, min(sphericalCoords(:,2))-.01, max(sphericalCoords(:,2))+.01])
    for i=1:size(cartesianCoords,1)
        text(sin(sphericalCoords(i,1)),sin(sphericalCoords(i,2)),num2str(i));
    end
    title('CellScope Dome v1.0 Hole Pattern Mapping');
    xlabel('front');
    ylabel('left side');
    viscircles([0 0], na);
    plot(sin(sphericalCoords(alignmentHoles,1)),sin(sphericalCoords(alignmentHoles,2)),'x');

end
%%
save('BrightfieldScanImageOrders.mat','globalBrightfieldList');





    